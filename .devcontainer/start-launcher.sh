#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
LAUNCHER_DIR="$ROOT_DIR/launcher"
LOG_DIR="$ROOT_DIR/.devcontainer/.logs"
LOG_FILE="$LOG_DIR/launcher.log"
PID_FILE="$LOG_DIR/launcher.pid"
FEATURE_LAUNCHER="$LAUNCHER_DIR/target/dependency/org.apache.sling.feature.launcher/bin/launcher"
FEATURE_JSON="$LAUNCHER_DIR/target/slingfeature-tmp/feature-slingslop_aggregate.json"
BUILD_LOG="$LOG_DIR/startup-build.log"

mkdir -p "$LOG_DIR"

start_launcher_process() {
	cd "$LAUNCHER_DIR"
	nohup ./launch.sh >"$LOG_FILE" 2>&1 &
	LAUNCHER_PID=$!
	echo "$LAUNCHER_PID" >"$PID_FILE"

	# Give the launcher a brief moment to fail fast on missing artifacts.
	sleep 3
	if kill -0 "$LAUNCHER_PID" >/dev/null 2>&1; then
		printf '%s\n' "Started Sling launcher (PID $LAUNCHER_PID) in background. Log: $LOG_FILE"
		return 0
	fi

	return 1
}

run_launcher_build() {
	printf '%s\n' 'Preparing launcher artifacts: mvn -q -pl launcher -am package -DskipITs'
	if (cd "$ROOT_DIR" && mvn -q -pl launcher -am package -DskipITs) >"$BUILD_LOG" 2>&1; then
		printf '%s\n' "Launcher artifact build succeeded. Log: $BUILD_LOG"
		return 0
	fi

	printf '%s\n' 'Launcher-only build was insufficient, trying full reactor install.'
	printf '%s\n' 'Running: mvn -q clean install -DskipITs'
	if (cd "$ROOT_DIR" && mvn -q clean install -DskipITs) >>"$BUILD_LOG" 2>&1; then
		printf '%s\n' "Full reactor build succeeded. Log: $BUILD_LOG"
		return 0
	fi

	printf '%s\n' "Automatic build failed. Inspect $BUILD_LOG" >&2
	tail -n 40 "$BUILD_LOG" >&2 || true
	return 1
}

handle_known_startup_failure() {
	if [ ! -f "$LOG_FILE" ]; then
		return 1
	fi

	if grep -q 'feature-slingslop_aggregate.json not found' "$LOG_FILE"; then
		printf '%s\n' 'Detected missing launcher feature model. Attempting auto-rebuild...'
		run_launcher_build
		return $?
	fi

	if grep -q 'not found in any repository' "$LOG_FILE"; then
		printf '%s\n' 'Detected missing local Maven artifacts. Attempting auto-rebuild...'
		run_launcher_build
		return $?
	fi

	return 1
}

if lsof -ti :8080 >/dev/null 2>&1; then
	printf '%s\n' 'Sling already listening on :8080; skipping auto-start.'
	exit 0
fi

if [ -f "$PID_FILE" ]; then
	PID=$(cat "$PID_FILE" || true)
	if [ -n "$PID" ] && kill -0 "$PID" >/dev/null 2>&1; then
		printf '%s\n' "Launcher already running with pid $PID; skipping auto-start."
		exit 0
	fi
	rm -f "$PID_FILE"
fi

if [ ! -x "$FEATURE_LAUNCHER" ] || [ ! -f "$FEATURE_JSON" ]; then
	printf '%s\n' 'Required launcher artifacts are missing. Attempting auto-rebuild...'
	run_launcher_build
fi

if start_launcher_process; then
	exit 0
fi

if handle_known_startup_failure; then
	printf '%s\n' 'Retrying launcher start after auto-rebuild...'
	if start_launcher_process; then
		exit 0
	fi
fi

printf '%s\n' 'Launcher exited during startup. Check the launcher log for details:' >&2
printf '%s\n' "  $LOG_FILE" >&2
tail -n 40 "$LOG_FILE" >&2 || true
exit 1
