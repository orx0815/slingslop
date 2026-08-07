#!/bin/sh
set -eu

URL="http://localhost:8080/content/sling-matrix/home.html"

if [ -z "${BROWSER:-}" ]; then
	printf '%s\n' 'BROWSER is not set; skipping automatic browser open.'
	exit 0
fi

attempt=0
while [ "$attempt" -lt 120 ]; do
	if curl -fsS "$URL" >/dev/null 2>&1; then
		"$BROWSER" "$URL" >/dev/null 2>&1 || true
		printf '%s\n' "Opened $URL"
		exit 0
	fi
	attempt=$((attempt + 1))
	sleep 2
done

printf '%s\n' "Sling did not become ready in time; open $URL manually if needed." >&2
	exit 1