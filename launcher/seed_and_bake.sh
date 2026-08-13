#!/bin/bash
# seed_and_bake.sh
#
# Produces a composite-mode Slingslop image:
#
#   1. Runs the base image once with the single-store slingslop_aggregate
#      feature against a throw-away local launcher directory. This populates
#      /apps and /libs (and everything else) into a fresh SegmentNodeStore.
#   2. Stops the container cleanly so the segment store is checkpointed.
#   3. Copies the resulting repository/segmentstore into ./target/seed/.
#   4. Builds a derived image from Dockerfile.composite that bakes the seed
#      in at /opt/sling/seed-repository/segmentstore and sets the composite
#      aggregate as its default CMD.
#
# See docs/composite-nodestore.md for the rationale and verification steps.
#
# Usage:
#   cd launcher
#   ./seed_and_bake.sh
#
# Environment variables (all optional):
#   IMAGE_BASE       Base image tag built by mvn package (default ghcr.io/orx0815/slingslop:snapshot)
#   IMAGE_OUT        Composite image tag to produce      (default ghcr.io/orx0815/slingslop:composite)
#   SEED_DIR         Local seed directory                 (default ./target/seed)
#   SEED_TIMEOUT     Max seconds to wait for content      (default 180)
#   SEED_PROBE_URL   URL that returns 200 once Sling serves (default /starter.html)
#   SEED_SETTLE      Seconds to wait after the probe for the content-package
#                    (ui.apps) install to finish before snapshotting (default 30)
#   SEED_HTTP_PORT   Host port to bind for the probe      (default 18080)

set -euo pipefail

IMAGE_BASE="${IMAGE_BASE:-ghcr.io/orx0815/slingslop:snapshot}"
IMAGE_OUT="${IMAGE_OUT:-ghcr.io/orx0815/slingslop:composite}"
SEED_DIR="${SEED_DIR:-$(pwd)/target/seed}"
SEED_TIMEOUT="${SEED_TIMEOUT:-180}"
# Seed is code-only. Probe a deep /apps (ui.apps) resource that only exists once
# the app content-packages are actually installed — a DETERMINISTIC "apps deployed"
# signal. Do NOT use /starter.html: it goes 200 well before Composum installs the
# ui.apps, so on a slow CI runner the seed would be snapshotted with an incomplete
# /apps (-> composite mount consistency check fails -> repo won't start). SEED_SETTLE
# is a small extra margin for the other apps + segment checkpoint.
SEED_PROBE_URL="${SEED_PROBE_URL:-/apps/slingslop/zengarden/pages/homepage.json}"
SEED_SETTLE="${SEED_SETTLE:-20}"
SEED_HTTP_PORT="${SEED_HTTP_PORT:-18080}"

CONTAINER_NAME="slingslop-seed-$$"
SEED_VOLUME="slingslop-seed-vol-$$"

cleanup() {
    if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo "[INFO] Removing seed container ${CONTAINER_NAME}"
        docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
    fi
    if docker volume ls --format '{{.Name}}' | grep -q "^${SEED_VOLUME}$"; then
        echo "[INFO] Removing seed volume ${SEED_VOLUME}"
        docker volume rm "${SEED_VOLUME}" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT

echo "[INFO] Seed directory:        ${SEED_DIR}"
echo "[INFO] Base image:             ${IMAGE_BASE}"
echo "[INFO] Composite image (out):  ${IMAGE_OUT}"

# Fresh seed dir on the host (only used to receive 'docker cp' output later)
rm -rf "${SEED_DIR}"
mkdir -p "${SEED_DIR}"

# Use a Docker named volume (not a host bind-mount) so Docker initialises it
# from the image and the sling user inside the container owns it. Host bind
# mounts would inherit the host UID and the in-container user would not be
# able to write into them.
docker volume create "${SEED_VOLUME}" >/dev/null

echo
echo "[STEP 1/4] Starting seed container (single-store mode)"
docker run -d \
    --name "${CONTAINER_NAME}" \
    -p "${SEED_HTTP_PORT}:8080" \
    -v "${SEED_VOLUME}:/opt/sling/launcher" \
    "${IMAGE_BASE}" \
    slingslop_aggregate >/dev/null

echo
echo "[STEP 2/4] Waiting up to ${SEED_TIMEOUT}s for ${SEED_PROBE_URL}"
deadline=$(( $(date +%s) + SEED_TIMEOUT ))
ready=0
while [ "$(date +%s)" -lt "${deadline}" ]; do
    code=$(curl -s -o /dev/null -w '%{http_code}' \
        -u admin:admin "http://localhost:${SEED_HTTP_PORT}${SEED_PROBE_URL}" || true)
    if [ "${code}" = "200" ]; then
        ready=1
        break
    fi
    sleep 2
done

if [ "${ready}" -ne 1 ]; then
    echo "[ERROR] Seed probe ${SEED_PROBE_URL} did not return 200 within ${SEED_TIMEOUT}s"
    docker logs --tail 200 "${CONTAINER_NAME}" || true
    exit 1
fi
echo "[INFO] Seed probe OK; settling ${SEED_SETTLE}s for content-package (ui.apps) install to finish"
sleep "${SEED_SETTLE}"

echo
echo "[STEP 3/4] Stopping container cleanly (so segment store is checkpointed)"
docker stop --time 30 "${CONTAINER_NAME}" >/dev/null

# Copy the seeded segmentstore out of the stopped container's view of the
# named volume. docker cp on a stopped container works and preserves perms.
echo "[INFO] Extracting repository/segmentstore to ${SEED_DIR}/segmentstore"
docker cp \
    "${CONTAINER_NAME}:/opt/sling/launcher/repository/segmentstore" \
    "${SEED_DIR}/segmentstore"

if [ ! -d "${SEED_DIR}/segmentstore" ]; then
    echo "[ERROR] No segmentstore directory at ${SEED_DIR}/segmentstore after docker cp"
    exit 1
fi
echo "[INFO] Captured seed segmentstore: $(du -sh "${SEED_DIR}/segmentstore" | cut -f1)"

echo
echo "[STEP 4/4] Building composite image ${IMAGE_OUT}"
docker build \
    -f Dockerfile.composite \
    --build-arg "BASE_IMAGE=${IMAGE_BASE}" \
    -t "${IMAGE_OUT}" \
    .

echo
echo "[DONE] Composite image ready: ${IMAGE_OUT}"
echo "       Run it with:"
echo "         docker run --rm -p 8080:8080 -v slingslop_content:/opt/sling/launcher ${IMAGE_OUT}"
