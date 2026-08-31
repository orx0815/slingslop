#!/bin/sh
# flush.cgi — internal cache-flush endpoint for the webcache.
#
# Served ONLY on the :8088 listener (see flush.conf), which is deliberately not
# published to the host and not routed by Traefik, so only peers on the docker
# network (e.g. the Sling container calling http://webcache:8088/flush) can
# reach it. That network isolation is the primary control; an optional shared
# secret (FLUSH_TOKEN env + X-Flush-Token header) adds defence-in-depth.
#
#   GET|POST /flush?prefix=/apps/sling-matrix/
#
# The heavy lifting is delegated to purge-cache.sh (URL-substring eviction).
set -eu

reply() {
  # $1 = "Status text", $2 = body
  printf 'Status: %s\r\nContent-Type: text/plain\r\nCache-Control: no-store\r\n\r\n%s\n' "$1" "$2"
  exit 0
}

# Optional shared secret — enforced only when FLUSH_TOKEN is set in the env.
if [ -n "${FLUSH_TOKEN:-}" ] && [ "${HTTP_X_FLUSH_TOKEN:-}" != "${FLUSH_TOKEN}" ]; then
  reply "403 Forbidden" "missing or invalid X-Flush-Token"
fi

# Pull prefix= out of the query string. Slashes are valid unencoded in a query
# value, so callers send the path verbatim (no %2F).
prefix=$(printf '%s' "${QUERY_STRING:-}" | sed -n 's/.*prefix=\([^&]*\).*/\1/p')

[ -n "$prefix" ] || reply "400 Bad Request" "usage: /flush?prefix=/apps/... (or /content/...)"

# Allowlist: an absolute path under /apps or /content, safe charset only. This
# scopes the blast radius AND prevents grep/shell metacharacter injection into
# purge-cache.sh (the value becomes a grep pattern).
case "$prefix" in
  /apps/*|/content/*) : ;;
  *) reply "400 Bad Request" "prefix must start with /apps/ or /content/" ;;
esac
case "$prefix" in
  *[!A-Za-z0-9/_.-]*) reply "400 Bad Request" "illegal characters in prefix (send it un-encoded)" ;;
esac

out=$(/usr/local/bin/purge-cache.sh "$prefix" 2>&1 || true)
reply "200 OK" "flushed prefix: ${prefix}
${out}"
