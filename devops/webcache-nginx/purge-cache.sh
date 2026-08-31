#!/bin/sh
# purge-cache.sh (nginx variant) — evict cached entries by URL substring.
#
# Unlike Apache's mod_cache_disk (paired <hash>.header + <hash>.data), nginx
# stores each entry as a SINGLE file whose head contains a "KEY: <cache-key>"
# line. The CONGA template's cache key is "$scheme$request_method$host$uri", so
# the URL path is in the KEY — grep for the substring (fixed-string) and delete
# matching files. Safe live: a deleted entry is just a MISS next time, no reload.
#
# CACHE_ROOT defaults to the PARENT of all per-tenant zones (CONGA's
# app.conf.hbs uses one zone dir per tenant, /var/cache/nginx/<tenant>), so this
# scans across every tenant — the substring itself (e.g. /apps/sling-matrix/)
# naturally scopes the match to the right one.
#
#   purge-cache.sh /apps/
#   purge-cache.sh /apps/sling-matrix/
#   purge-cache.sh /content/sling-matrix/home.html      # flush-this-page
set -eu

CACHE_ROOT="${CACHE_ROOT:-/var/cache/nginx}"
pattern="${1:?usage: purge-cache.sh <url-substring>}"

[ -d "$CACHE_ROOT" ] || { echo "purge-cache: no cache root at $CACHE_ROOT"; exit 0; }

# -F fixed string (URL paths contain dots); -a treat binary as text; -l list.
grep -rlaF -- "$pattern" "$CACHE_ROOT" 2>/dev/null | while IFS= read -r f; do
  rm -f "$f"
  echo "purged: ${f#"$CACHE_ROOT"/}"
done

echo "purge-cache(nginx): done (pattern='$pattern', root='$CACHE_ROOT')"
