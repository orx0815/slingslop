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
#   READY_TIMEOUT       seconds to wait for the Composum package manager  default 180
set -eu

SLING_URL="${SLING_URL:-http://localhost:8080}"
SLING_USER="${SLING_USER:-admin}"
SLING_PASSWORD="${SLING_PASSWORD:-admin}"
SAMPLE_CONTENT_DIR="${SAMPLE_CONTENT_DIR:-/opt/sling/sample-content}"
READY_TIMEOUT="${READY_TIMEOUT:-180}"

AUTH="${SLING_USER}:${SLING_PASSWORD}"

if [ ! -d "${SAMPLE_CONTENT_DIR}" ]; then
  echo "[sample-content] ${SAMPLE_CONTENT_DIR} not found, nothing to install"
  exit 0
fi

# Wait for Composum's package manager itself to be ready — the operation we need,
# not just /starter.html (which goes 200 well before the package-manager servlet
# registers). package.list.json returns 200 once the PackagesServlet is up; a 401
# means it IS up but the credentials are wrong (fail fast — that won't self-heal).
deadline=$(( $(date +%s) + READY_TIMEOUT ))
while :; do
  code=$(curl -s -o /dev/null -w '%{http_code}' -u "${AUTH}" "${SLING_URL}/bin/cpm/package.list.json")
  case "${code}" in
    200) break ;;
    401|403) echo "[sample-content] authentication failed (HTTP ${code}) as ${SLING_USER} at ${SLING_URL} — check SLING_PASSWORD" >&2; exit 1 ;;
    *)
      if [ "$(date +%s)" -ge "${deadline}" ]; then
        echo "[sample-content] Composum package manager not ready (last HTTP ${code}) within ${READY_TIMEOUT}s at ${SLING_URL}" >&2
        exit 1
      fi
      sleep 3 ;;
  esac
done

# Upload one package, retrying while Composum's package-manager servlet is still
# coming up. /starter.html goes 200 well before Composum registers, so during the
# window the endpoint transiently returns 000/404/500/503 — keep retrying.
upload_pkg() {
  zip="$1"; d=$(( $(date +%s) + READY_TIMEOUT ))
  while :; do
    code=$(curl -s --connect-timeout 10 --max-time 180 \
            -o /tmp/sc-upload.json -w '%{http_code}' -u "${AUTH}" \
            -F "file=@${zip}" -F force=true "${SLING_URL}/bin/cpm/package.upload.json")
    if [ "${code}" = "200" ]; then sed -n 's/.*"path":"\([^"]*\)".*/\1/p' /tmp/sc-upload.json; return 0; fi
    case "${code}" in
      000|404|500|503)
        [ "$(date +%s)" -lt "${d}" ] || { echo "[sample-content] package manager still ${code} after ${READY_TIMEOUT}s for ${zip}" >&2; cat /tmp/sc-upload.json >&2 || true; return 1; }
        sleep 3; continue ;;
      *)
        echo "[sample-content] upload failed (HTTP ${code}) for ${zip}" >&2; cat /tmp/sc-upload.json >&2 || true; return 1 ;;
    esac
  done
}

# Install an already-uploaded package, retrying on the same transient codes as
# the upload. Even after the readiness gate the instance may still be warming up
# (bundles activating, node types/namespaces not yet registered), so a fire-once
# install can transiently 500/503 at cold start though it succeeds moments later.
install_pkg() {
  path="$1"; d=$(( $(date +%s) + READY_TIMEOUT ))
  while :; do
    code=$(curl -s --connect-timeout 10 --max-time 300 \
            -o /tmp/sc-install.json -w '%{http_code}' -u "${AUTH}" -X POST \
            "${SLING_URL}/bin/cpm/package.install.json${path}")
    if [ "${code}" = "200" ]; then return 0; fi
    case "${code}" in
      000|404|500|503)
        [ "$(date +%s)" -lt "${d}" ] || { echo "[sample-content] install still ${code} after ${READY_TIMEOUT}s for ${path}" >&2; cat /tmp/sc-install.json >&2 || true; return 1; }
        sleep 3; continue ;;
      *)
        echo "[sample-content] install failed (HTTP ${code}) for ${path}" >&2; cat /tmp/sc-install.json >&2 || true; return 1 ;;
    esac
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

  install_pkg "${P}" || { rc=1; continue; }
  echo "[sample-content] installed ${P}"
done

[ "${found}" = 1 ] || echo "[sample-content] no *.zip in ${SAMPLE_CONTENT_DIR}"
exit ${rc}
