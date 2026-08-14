#!/bin/sh -eu
# install-sample-content-local.sh
#
# Installs the demo sample-content packages into the locally-running Sling
# instance started by launch.sh. Sample-content is NOT baked into the
# feature/seed (code-only image); it is (re)installed into the running
# instance instead — same principle as the container/prod path
# (src/main/container/bin/install-sample-content.sh), but driven through the
# wcm.io content-package Maven plugin rather than curl: it uploads to Composum
# via the serviceURL configured in the parent pom and has built-in retry
# (retryCount/retryDelay) that rides out the cold-start window before the
# package-manager servlet registers. The AEM-specific readiness probes are
# disabled (-) since they don't apply to plain Sling+Composum.
#
# Run from the launcher/ directory, after the feature launcher has been
# started (intended to be backgrounded so it can wait while Sling comes up).
#
# Env:
#   SAMPLE_CONTENT_DIR  default target/sample-content (dir of *.zip packages)

SAMPLE_CONTENT_DIR="${SAMPLE_CONTENT_DIR:-target/sample-content}"

if [ ! -d "${SAMPLE_CONTENT_DIR}" ]; then
  echo "[sample-content] ${SAMPLE_CONTENT_DIR} not found, nothing to install"
  exit 0
fi

SAMPLE_ZIPS=$(ls "$(pwd)/${SAMPLE_CONTENT_DIR}"/*.zip 2>/dev/null | paste -sd, - || true)
if [ -z "$SAMPLE_ZIPS" ]; then
  echo "[sample-content] no *.zip packages found in ${SAMPLE_CONTENT_DIR}, nothing to install"
  exit 0
fi

mvn -N wcmio-content-package:install \
    -Dvault.fileList="$SAMPLE_ZIPS" \
    -Dvault.force=true \
    -Dvault.retryCount=40 \
    -Dvault.bundleStatusURL=- \
    -Dvault.packageManagerInstallStatusURL=- \
    -Dvault.systemReadyURL=-
