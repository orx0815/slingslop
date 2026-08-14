#!/usr/bin/env bash
# vt-bench.sh — Virtual-thread vs platform-thread load benchmark
#
# Drives VtBenchProbeFilter (sling-apps/sling-bench/vt-bench-probe.core), an
# app-independent Servlet Filter registered on the Sling filter whiteboard
# (sling.filter.scope=REQUEST). It runs BEFORE resource resolution, so it works
# against ANY URL on the instance — no per-app resourceType script or Sling
# Model needed. Disabled by default; see the "enable the probe" step below.
#
# Usage: ./vt-bench.sh [BASE_URL] [DELAY_MS] [DURATION_SECS] [CONCURRENCIES]
#   BASE_URL       default http://localhost:8080
#   DELAY_MS       simulated I/O wait per request, default 100
#   DURATION_SECS  wrk/hey run length per concurrency level, default 10
#   CONCURRENCIES  comma-separated list, default "10,50,200,500"
#
# Prerequisites (one of):
#   wrk  — apt install wrk  /  brew install wrk
#   hey  — go install github.com/rakyll/hey@latest  /  apt install hey
#
# Enable the probe (OFF by default — this is a load-testing tool, not something
# to leave on in production):
#   http://localhost:8080/system/console/configMgr → "VT Bench Probe" → Enabled
#   (PID org.motorbrot.vtbenchprobe.VtBenchProbeFilter)
#
# The Sling thread-pool ceiling is httpThreadpoolMax (default 200 in local-plain.yaml).
# Virtual threads park on Thread.sleep; platform threads hold an OS thread.
# Drive concurrency above the pool ceiling to see the difference.
#
# Example (compare 100ms I/O at 500 concurrent):
#   httpVirtualThreads: false  →  queue fills, p99 spikes, RPS plateaus
#   httpVirtualThreads: true   →  throughput scales, p99 stays flat
#
# Steps to flip the config:
#   1. Edit devops/conga/src/main/environments/local-plain.yaml
#      httpVirtualThreads: false   (or true)
#   2. Regenerate CONGA:  cd devops/conga && mvn generate-resources
#   3. Restart Sling:     cd launcher && ./launch.sh
#   4. Re-run this script — results are timestamped so both runs are preserved.

set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
DELAY_MS="${2:-100}"
DURATION="${3:-10}"
CONCURRENCIES_ARG="${4:-10,50,200,500}"

IFS=',' read -ra CONCURRENCIES <<< "$CONCURRENCIES_ARG"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="$SCRIPT_DIR/benchmark/results"
mkdir -p "$RESULTS_DIR"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
RESULT_FILE="$RESULTS_DIR/vt-bench-${TIMESTAMP}.txt"

# ── tool detection ─────────────────────────────────────────────────────────────
if command -v wrk &>/dev/null; then
    TOOL="wrk"
elif command -v hey &>/dev/null; then
    TOOL="hey"
else
    echo "ERROR: install wrk (preferred) or hey before running this script."
    echo "  apt install wrk     or     brew install wrk"
    echo "  go install github.com/rakyll/hey@latest"
    exit 1
fi

# ── example target paths — the probe itself is app-independent; these just
#    happen to be the richest existing content tree in this mono-repo. Swap in
#    any real paths from any app, or even nonexistent ones (with the default
#    renderRealPage=false the filter short-circuits before resource resolution). ─
PAGES=(
    "/content/sling-matrix/home"
    "/content/sling-matrix/home/osgi/osgi-intro"
    "/content/sling-matrix/home/osgi/osgi-ds"
    "/content/sling-matrix/home/osgi/osgi-testing"
    "/content/sling-matrix/home/jcr/jcr-intro"
    "/content/sling-matrix/home/jcr/jcr-queries"
    "/content/sling-matrix/home/sling/sling-intro"
    "/content/sling-matrix/home/sling/sling-dml-demo"
    "/content/sling-matrix/home/sling/sling-models"
    "/content/sling-matrix/home/sling/sling-resource-api"
    "/content/sling-matrix/home/sling/sling-components"
    "/content/sling-matrix/home/sling/sling-caconfig"
    "/content/sling-matrix/home/sling/sling-testing"
    "/content/sling-matrix/home/sling/sling-i18n"
    "/content/sling-matrix/home/sling/htl-sightly"
)

# Probe query param: /content/…/page.html?_vtbenchDelay=<ms>
make_url() {
    echo "${BASE_URL}${1}.html?_vtbenchDelay=${DELAY_MS}"
}

# ── sanity-check that Sling is up AND the probe is actually enabled ────────────
# A disabled probe still returns HTTP 200 (the real page just renders normally),
# so checking the status code alone would silently "pass" with no delay ever
# applied. Check for the X-VT-Bench-Thread response header instead — proof the
# filter actually fired.
PROBE_URL=$(make_url "${PAGES[0]}")
PROBE_HEADERS=$(curl -s -D- -o /dev/null "$PROBE_URL" || true)
HTTP_STATUS=$(echo "$PROBE_HEADERS" | head -1 | grep -oE '[0-9]{3}' || true)
if [[ "$HTTP_STATUS" != "200" ]]; then
    echo "ERROR: probe returned HTTP $HTTP_STATUS for $PROBE_URL"
    echo "  Is Sling running at $BASE_URL?"
    exit 1
fi
if ! echo "$PROBE_HEADERS" | grep -qi '^X-VT-Bench-Thread:'; then
    echo "ERROR: no X-VT-Bench-Thread header in the response — VtBenchProbeFilter is not enabled."
    echo "  Enable it at ${BASE_URL}/system/console/configMgr -> \"VT Bench Probe\" -> Enabled"
    echo "  (PID org.motorbrot.vtbenchprobe.VtBenchProbeFilter), then re-run this script."
    exit 1
fi

# ── detect VT from CONGA config ────────────────────────────────────────────────
CONGA_ENV="$SCRIPT_DIR/../../devops/conga/src/main/environments/local-plain.yaml"
VT_SETTING="unknown"
if [[ -f "$CONGA_ENV" ]]; then
    VT_SETTING=$(grep -E 'httpVirtualThreads' "$CONGA_ENV" | awk '{print $2}' | tr -d '\r' || echo "unknown")
fi

# ── CPU/MEM sampler ────────────────────────────────────────────────────────────
JAVA_PID=$(pgrep -f 'org.apache.sling.feature.launcher' | head -1 || true)
CPU_LOG="$RESULTS_DIR/cpu-${TIMESTAMP}.csv"
start_sampler() {
    if [[ -n "$JAVA_PID" ]]; then
        echo "timestamp,cpu%,rss_mb" > "$CPU_LOG"
        while kill -0 "$JAVA_PID" 2>/dev/null; do
            local line
            line=$(ps -p "$JAVA_PID" -o %cpu=,rss= 2>/dev/null || true)
            if [[ -n "$line" ]]; then
                local cpu rss_mb
                cpu=$(awk '{print $1}' <<< "$line")
                rss_mb=$(awk '{printf "%.0f", $2/1024}' <<< "$line")
                echo "$(date +%H:%M:%S),$cpu,$rss_mb" >> "$CPU_LOG"
            fi
            sleep 2
        done &
        SAMPLER_PID=$!
    fi
}
stop_sampler() {
    [[ -n "${SAMPLER_PID:-}" ]] && kill "$SAMPLER_PID" 2>/dev/null || true
}
SAMPLER_PID=""
trap stop_sampler EXIT

# ── benchmark runners ──────────────────────────────────────────────────────────
# wrk: one process, Lua round-robin across all page URLs — zero inter-process
# CPU competition with the Sling JVM.
run_wrk_all() {
    local conc=$1
    local threads=$(( conc < 4 ? conc : 4 ))
    local lua
    lua=$(mktemp --suffix=.lua)
    {
        echo 'local urls = {'
        for PAGE in "${PAGES[@]}"; do
            echo "  \"$(make_url "$PAGE")\","
        done
        echo '}'
        # idx is per-connection; races between wrk threads are harmless here
        echo 'local idx = 0'
        echo 'request = function()'
        echo '  idx = (idx % #urls) + 1'
        echo '  return wrk.format("GET", urls[idx])'
        echo 'end'
    } > "$lua"
    wrk -t "$threads" -c "$conc" -d "${DURATION}s" -s "$lua" "${BASE_URL}/" 2>&1
    rm -f "$lua"
}

# hey fallback: sequential pages — avoids spawning 15 competing processes.
run_hey_sequential() {
    local conc=$1
    for PAGE in "${PAGES[@]}"; do
        echo ""
        echo "  Page: $PAGE"
        hey -c "$conc" -z "${DURATION}s" "$(make_url "$PAGE")" 2>&1
    done
}

# ── header ─────────────────────────────────────────────────────────────────────
{
cat <<EOF
=============================================================================
 vt-bench  |  $(date)
 tool      : $TOOL
 base_url  : $BASE_URL
 delay_ms  : $DELAY_MS  (simulated blocking I/O per request, via VtBenchProbeFilter)
 duration  : ${DURATION}s per concurrency level
 httpVirtualThreads: $VT_SETTING  (from local-plain.yaml)
 java_pid  : ${JAVA_PID:-not found}
 pages     : ${#PAGES[@]}
=============================================================================
NOTE: httpThreadpoolMax is 200 in local-plain.yaml.
      Concurrency > 200 is where virtual threads pull ahead.
      To compare, flip httpVirtualThreads, regenerate CONGA, restart, re-run.
=============================================================================

EOF
} | tee "$RESULT_FILE"

# ── start CPU sampler ──────────────────────────────────────────────────────────
start_sampler

# ── rounds ─────────────────────────────────────────────────────────────────────
for CONC in "${CONCURRENCIES[@]}"; do
    echo "" | tee -a "$RESULT_FILE"
    if [[ "$TOOL" == "wrk" ]]; then
        echo "─── concurrency = $CONC  (${#PAGES[@]} URLs round-robin, ${DURATION}s) ──────" | tee -a "$RESULT_FILE"
        echo "" | tee -a "$RESULT_FILE"
        run_wrk_all "$CONC" | tee -a "$RESULT_FILE"
    else
        echo "─── concurrency = $CONC  (${#PAGES[@]} pages sequential, ${DURATION}s each) ──" | tee -a "$RESULT_FILE"
        run_hey_sequential "$CONC" | tee -a "$RESULT_FILE"
    fi
done

# ── CPU/MEM summary ────────────────────────────────────────────────────────────
stop_sampler
if [[ -f "$CPU_LOG" ]] && (( $(wc -l < "$CPU_LOG") > 1 )); then
    echo "" | tee -a "$RESULT_FILE"
    echo "─── CPU / RSS samples (every 2s, sling JVM pid $JAVA_PID) ──────────" | tee -a "$RESULT_FILE"
    # Print min/max/avg CPU and peak RSS
    awk -F',' 'NR>1 {
        cpu+=$2; if($2>maxcpu)maxcpu=$2; if(min==""||$2<min)min=$2;
        if($3>maxrss)maxrss=$3; count++
    }
    END {
        printf "  avg_cpu=%.1f%%  min_cpu=%.1f%%  max_cpu=%.1f%%  peak_rss=%dMB  samples=%d\n",
               cpu/count, min, maxcpu, maxrss, count
    }' "$CPU_LOG" | tee -a "$RESULT_FILE"
    echo "  (full CPU log: $CPU_LOG)" | tee -a "$RESULT_FILE"
fi

echo "" | tee -a "$RESULT_FILE"
echo "Results written to: $RESULT_FILE" | tee -a "$RESULT_FILE"
