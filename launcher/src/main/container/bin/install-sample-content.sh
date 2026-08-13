#!/bin/bash
# Install the demo sample-content packages into a RUNNING Sling via the Composum
# package manager. Demo content is ALWAYS re-applied (force) on every launch /
# deploy; it is NOT baked into the composite seed (which is code-only). Each
# package filter is scoped to its own /content/... root, so user content on other
# roots (e.g. the content-only 'zen' tenant) is never touched.
#
# Env:
#   SLING_URL           default http://localhost:8080
#   SLING_USER          default admin
#   SLING_PASSWORD      default admin
#   SAMPLE_CONTENT_DIR  default /opt/sling/sample-content (dir of *.zip packages)
#   READY_TIMEOUT       seconds to wait for Sling HTTP    default 120
set -eu

SLING_URL="${SLING_URL:-http://localhost:8080}"
SLING_USER="${SLING_USER:-admin}"
SLING_PASSWORD="${SLING_PASSWORD:-admin}"
SAMPLE_CONTENT_DIR="${SAMPLE_CONTENT_DIR:-/opt/sling/sample-content}"
READY_TIMEOUT="${READY_TIMEOUT:-120}"

AUTH="${SLING_USER}:${SLING_PASSWORD}"

if [ ! -d "${SAMPLE_CONTENT_DIR}" ]; then
  echo "[sample-content] ${SAMPLE_CONTENT_DIR} not found, nothing to install"
  exit 0
fi

# Wait for Sling to actually serve (200), not just accept the socket.
deadline=$(( $(date +%s) + READY_TIMEOUT ))
until [ "$(curl -s -o /dev/null -w '%{http_code}' "${SLING_URL}/starter.html")" = "200" ]; do
  if [ "$(date +%s)" -ge "${deadline}" ]; then
    echo "[sample-content] Sling did not become ready within ${READY_TIMEOUT}s at ${SLING_URL}" >&2
    exit 1
  fi
  sleep 3
done

# Upload one package, retrying while the Composum package manager is still warming
# up (503) or briefly unreachable (000). Echoes the uploaded package path on 200.
upload_pkg() {
  zip="$1"; d=$(( $(date +%s) + READY_TIMEOUT ))
  while :; do
    code=$(curl -s --connect-timeout 10 --max-time 180 \
            -o /tmp/sc-upload.json -w '%{http_code}' -u "${AUTH}" \
            -F "file=@${zip}" -F force=true "${SLING_URL}/bin/cpm/package.upload.json")
    if [ "${code}" = "200" ]; then sed -n 's/.*"path":"\([^"]*\)".*/\1/p' /tmp/sc-upload.json; return 0; fi
    if [ "${code}" = "503" ] || [ "${code}" = "000" ]; then
      [ "$(date +%s)" -lt "${d}" ] || { echo "[sample-content] package manager still ${code} after ${READY_TIMEOUT}s for ${zip}" >&2; return 1; }
      sleep 3; continue
    fi
    echo "[sample-content] upload failed (HTTP ${code}) for ${zip}" >&2; cat /tmp/sc-upload.json >&2 || true; return 1
  done
}

rc=0
found=0
for zip in "${SAMPLE_CONTENT_DIR}"/*.zip; do
  [ -e "${zip}" ] || break
  found=1
  echo "[sample-content] installing $(basename "${zip}")"

  P=$(upload_pkg "${zip}") || { rc=1; continue; }
  if [ -z "${P}" ]; then
    echo "[sample-content] upload returned no package path for ${zip}" >&2
    rc=1; continue
  fi

  code=$(curl -s --connect-timeout 10 --max-time 300 \
          -o /tmp/sc-install.json -w '%{http_code}' -u "${AUTH}" -X POST \
          "${SLING_URL}/bin/cpm/package.install.json${P}")
  if [ "${code}" != "200" ]; then
    echo "[sample-content] install failed (HTTP ${code}) for ${P}" >&2
    cat /tmp/sc-install.json >&2 || true
    rc=1; continue
  fi
  echo "[sample-content] installed ${P}"
done

[ "${found}" = 1 ] || echo "[sample-content] no *.zip in ${SAMPLE_CONTENT_DIR}"
exit ${rc}
