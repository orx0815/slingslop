#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
LAUNCHER_DIR="$ROOT_DIR/launcher"
LOG_DIR="$ROOT_DIR/.devcontainer/.logs"
LOG_FILE="$LOG_DIR/launcher.log"
PID_FILE="$LOG_DIR/launcher.pid"
FEATURE_LAUNCHER="$LAUNCHER_DIR/target/dependency/org.apache.sling.feature.launcher/bin/launcher"

mkdir -p "$LOG_DIR"

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

if [ ! -x "$FEATURE_LAUNCHER" ]; then
	printf '%s\n' 'Launcher binary missing; run `mvn clean install -DskipITs` first.' >&2
	exit 1
fi

cd "$LAUNCHER_DIR"
nohup ./launch.sh >"$LOG_FILE" 2>&1 &
LAUNCHER_PID=$!
echo "$LAUNCHER_PID" >"$PID_FILE"

printf '%s\n' "Started Sling launcher (PID $LAUNCHER_PID) in background. Log: $LOG_FILE"