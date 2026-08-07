#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
SLING_APPS_DIR="$ROOT_DIR/sling-apps"
ROOT_URL="${OPEN_SLING_ROOT_URL:-http://localhost:8080/}"
URL_OVERRIDE="${OPEN_SLING_URL:-}"
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

to_app_home_candidates() {
	app="$1"
	printf '%s\n' "$ROOT_URL""content/""$app""/home.html"
	printf '%s\n' "$ROOT_URL""content/slingslop/""$app""/home.html"
}

collect_candidates() {
	if [ -n "${OPEN_SLING_APP:-}" ]; then
		to_app_home_candidates "$OPEN_SLING_APP"
	fi

	if [ -d "$SLING_APPS_DIR" ]; then
		for app_dir in $(ls -1dt "$SLING_APPS_DIR"/*/ 2>/dev/null || true); do
			app=$(basename "$app_dir")
			case "$app" in
				zengarden|sling-matrix)
					continue
					;;
			esac
			to_app_home_candidates "$app"
		done
	fi

	to_app_home_candidates "sling-matrix"
	to_app_home_candidates "zengarden"
}

resolve_first_ready_url() {
	for candidate in $(collect_candidates); do
		if curl -fsS "$candidate" >/dev/null 2>&1; then
			printf '%s\n' "$candidate"
			return 0
		fi
	done
	return 1
}

attempt=0
while [ "$attempt" -lt "$MAX_ATTEMPTS" ]; do
	if [ -n "$URL_OVERRIDE" ]; then
		if curl -fsS "$URL_OVERRIDE" >/dev/null 2>&1; then
			"$BROWSER" "$URL_OVERRIDE" >/dev/null 2>&1 || true
			printf '%s\n' "Opened $URL_OVERRIDE"
			exit 0
		fi
	else
		if URL=$(resolve_first_ready_url); then
			"$BROWSER" "$URL" >/dev/null 2>&1 || true
			printf '%s\n' "Opened $URL"
			exit 0
		fi
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
	if [ -n "$URL_OVERRIDE" ]; then
		printf '%s\n' "Sling is up, but $URL_OVERRIDE is not ready yet. Open manually if needed." >&2
	else
		printf '%s\n' 'Sling is up, but no app homepage was ready yet. Open manually if needed.' >&2
	fi
	printf '%s\n' "To wait longer next time: OPEN_MATRIX_WAIT_SECONDS=600 .devcontainer/open-matrix.sh" >&2
	exit 1
fi

printf '%s\n' "Sling did not become ready in time on $ROOT_URL." >&2
printf '%s\n' "Check launcher logs: $LOG_FILE" >&2
printf '%s\n' "To wait longer next time: OPEN_MATRIX_WAIT_SECONDS=600 .devcontainer/open-matrix.sh" >&2
exit 1
