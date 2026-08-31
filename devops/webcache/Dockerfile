# Pull base image.
FROM ubuntu:latest

# Avoid interaction with tzdata
ARG DEBIAN_FRONTEND=noninteractive

# Install Apache (curl is used by the compose healthcheck and deploy readiness probe)
RUN apt-get -y update && apt-get -y install apache2 curl && \
    rm -rf /var/lib/apt/lists/*

# Configure mod_rewrite
RUN a2enmod rewrite

# Configure mod_proxy
RUN a2enmod proxy
RUN a2enmod proxy_http

# Configure mod_cache and mod_expire
RUN a2enmod cache
RUN a2enmod cache_disk
RUN a2enmod expires
RUN a2enmod headers

# Configure mod_cgid for the internal cache-flush endpoint (flush.conf)
RUN a2enmod cgid

# Configure sites
RUN mkdir -p /var/www/vhosts/www && \
    mkdir -p /var/www/vhosts/zengarden && \
    mkdir -p /var/cache/httpd/slingslop && \
    chown -R www-data:www-data /var/cache/httpd

# No vhosts baked in — docker-compose.yml bind-mounts the CONGA-generated
# zengarden/www configs into /etc/apache2/sites-enabled at run time.

# Internal cache-flush endpoint: lives in conf-enabled (NOT sites-enabled,
# which is entirely replaced by the bind-mounted CONGA vhosts at runtime).
# Listens on :8088 — see flush.conf for why that's never published/routed.
COPY flush.conf /etc/apache2/conf-available/flush.conf
RUN a2enconf flush

# Selective cache-purge helper (evict by URL substring, e.g. /apps/) — used by
# the flush endpoint, and reusable standalone (docker exec ... purge-cache.sh).
COPY purge-cache.sh /usr/local/bin/purge-cache.sh
RUN chmod +x /usr/local/bin/purge-cache.sh

# Internal cache-flush CGI (exposed only on the :8088 listener via flush.conf)
COPY flush.cgi /usr/lib/cgi-bin/flush
RUN chmod +x /usr/lib/cgi-bin/flush

# Expose endpoints
EXPOSE 80
EXPOSE 8088

# Start Apache httpd
RUN service apache-htcacheclean start
CMD ["/usr/sbin/apache2ctl", "-DFOREGROUND"]
