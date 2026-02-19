# Pull base image.
FROM ubuntu:latest

# Avoid interaction with tzdata
ARG DEBIAN_FRONTEND=noninteractive

# Install Apache
RUN apt-get -y update && apt-get -y install apache2

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
RUN mkdir -p /var/www/vhosts/editor.motorbrot &&  mkdir -p /var/www/vhosts/www && \
    mkdir -p /var/cache/httpd/slingslop && \
    chown -R www-data:www-data /var/cache/httpd

COPY *.conf /etc/apache2/sites-enabled/

# Expose endpoints
EXPOSE 80

# Start Apache httpd
RUN service apache-htcacheclean start
CMD ["/usr/sbin/apache2ctl", "-DFOREGROUND"]
