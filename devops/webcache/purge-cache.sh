#!/bin/sh
# purge-cache.sh — selectively evict mod_cache_disk entries by URL substring,
# without nuking the whole cache volume.
#
# WHY NOT `rm -rf .../apps/`?
#   mod_cache_disk names files by a HASH of the cache key, laid out under
#   CacheDirLevels directories (e.g. CACHE_ROOT/a/b/<hash>.header + .data).
#   There is no URL-path directory structure on disk, so you cannot delete a
#   "/apps/" subtree directly.
#
# HOW THIS WORKS
#   Every entry is a <hash>.header + <hash>.data pair; the .header stores the
#   original request URL (the cache key). We grep the .header files for the
#   given substring and delete both files of each match. This also catches all
#   Vary: Accept-Encoding variants of a URL, since they share the same key text.
#   Deleting cache files while Apache runs is safe: the next request is a miss
#   and repopulates from origin. No reload required.
#
# USAGE
#   purge-cache.sh '/apps/'                            # all static app assets
#   purge-cache.sh '/apps/sling-matrix/'              # one app's assets
#   purge-cache.sh '/content/sling-matrix/home.html'  # a single page
#
# ENV
#   CACHE_ROOT  cache directory (default /var/cache/httpd/slingslop)
set -eu

CACHE_ROOT="${CACHE_ROOT:-/var/cache/httpd/slingslop}"
pattern="${1:?usage: purge-cache.sh <url-substring> (e.g. /apps/)}"

if [ ! -d "$CACHE_ROOT" ]; then
  echo "purge-cache: no cache root at $CACHE_ROOT — nothing to do"
  exit 0
fi

# List .header files whose (binary) contents contain the URL substring, then
# drop the matching <hash>.header + <hash>.data pair. -a: treat as text; -l:
# just the filename. mod_cache_disk hash filenames never contain newlines, so
# a plain line loop is safe (and POSIX — the container's /bin/sh is dash).
find "$CACHE_ROOT" -type f -name '*.header' -exec grep -la -- "$pattern" {} + 2>/dev/null \
  | while IFS= read -r header; do
      base=${header%.header}
      rm -f "$base.header" "$base.data"
      echo "purged: ${header#"$CACHE_ROOT"/}"
    done

echo "purge-cache: done (pattern='$pattern', root='$CACHE_ROOT')"
