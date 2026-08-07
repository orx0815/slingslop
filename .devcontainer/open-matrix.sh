#!/bin/sh
set -eu

URL="http://localhost:8080/content/sling-matrix/home.html"
ROOT_URL="http://localhost:8080/"
LOG_FILE="/workspaces/slingslop/.devcontainer/.logs/launcher.log"

POLL_SECONDS=${OPEN_MATRIX_POLL_SECONDS:-2}
WAIT_SECONDS=${OPEN_MATRIX_WAIT_SECONDS:-300}

if [ "$POLL_SECONDS" -le 0 ]; then
	POLL_SECONDS=2
fi

MAX_ATTEMPTS=$((WAIT_SECONDS / POLL_SECONDS))
if [ "$MAX_ATTEMPTS" -le 0 ]; then
	MAX_ATTEMPTS=1
fi

if [ -z "${BROWSER:-}" ]; then
	printf '%s\n' 'BROWSER is not set; skipping automatic browser open.'
	exit 0
fi

attempt=0
while [ "$attempt" -lt "$MAX_ATTEMPTS" ]; do
	if curl -fsS "$URL" >/dev/null 2>&1; then
		"$BROWSER" "$URL" >/dev/null 2>&1 || true
		printf '%s\n' "Opened $URL"
		exit 0
	fi

	if [ -f "$LOG_FILE" ] && grep -q 'feature-slingslop_aggregate.json not found' "$LOG_FILE"; then
		printf '%s\n' 'Sling launcher failed: feature-slingslop_aggregate.json is missing.' >&2
		printf '%s\n' 'Run: mvn -q -pl launcher -am package -DskipITs' >&2
		exit 1
	fi

	if [ -f "$LOG_FILE" ] && grep -q 'not found in any repository' "$LOG_FILE"; then
		printf '%s\n' 'Sling launcher failed: required local Maven artifacts are missing.' >&2
		printf '%s\n' 'Run: mvn clean install -DskipITs' >&2
		exit 1
	fi

	attempt=$((attempt + 1))
	sleep "$POLL_SECONDS"
done

if curl -fsS "$ROOT_URL" >/dev/null 2>&1; then
	printf '%s\n' "Sling is up, but $URL is not ready yet. Open manually if needed." >&2
	printf '%s\n' "To wait longer next time: OPEN_MATRIX_WAIT_SECONDS=600 .devcontainer/open-matrix.sh" >&2
	exit 1
fi

printf '%s\n' "Sling did not become ready in time on $ROOT_URL." >&2
printf '%s\n' "Check launcher logs: $LOG_FILE" >&2
printf '%s\n' "To wait longer next time: OPEN_MATRIX_WAIT_SECONDS=600 .devcontainer/open-matrix.sh" >&2
exit 1
