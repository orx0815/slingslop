# nginx (OpenResty) webcache — feature-equivalent alternative to the Apache
# image (devops/webcache/), with the same internal cache-flush endpoint.
# OpenResty is used (not stock nginx) so the internal /flush endpoint can shell
# out via Lua; stock nginx open-source has no CGI and no native purge.
#
# Build:  docker build -t slingslop-webcache-nginx devops/webcache-nginx
#
# No per-tenant vhosts baked in — same convention as devops/webcache/: the
# generated CONGA output (one <tenant>.nginx.conf per public-cached app) is
# bind-mounted over /etc/nginx/conf.d at runtime.
FROM openresty/openresty:alpine

RUN mkdir -p /var/cache/nginx && \
    apk add --no-cache grep curl

COPY nginx.conf /usr/local/openresty/nginx/conf/nginx.conf

# Selective cache-purge helper (evict by URL substring, e.g. /apps/) — used by
# the flush endpoint below, and reusable standalone (docker exec ... purge-cache.sh).
COPY purge-cache.sh /usr/local/bin/purge-cache.sh
RUN chmod +x /usr/local/bin/purge-cache.sh

EXPOSE 80
# :8088 (internal flush) is intentionally NOT exposed to the host — reachable
# only on the docker network, same isolation model as the Apache image.
EXPOSE 8088

CMD ["/usr/local/openresty/bin/openresty", "-g", "daemon off;"]
