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

# Configure sites
RUN mkdir -p /var/www/vhosts/www && \
    mkdir -p /var/www/vhosts/zengarden && \
    mkdir -p /var/cache/httpd/slingslop && \
    chown -R www-data:www-data /var/cache/httpd

# No vhosts baked in — docker-compose.yml bind-mounts the CONGA-generated
# zengarden/www configs into /etc/apache2/sites-enabled at run time.

# Expose endpoints
EXPOSE 80

# Start Apache httpd
RUN service apache-htcacheclean start
CMD ["/usr/sbin/apache2ctl", "-DFOREGROUND"]
