# Adopted from https://github.com/apache/sling-org-apache-sling-starter/blob/master/Dockerfile

FROM docker.io/eclipse-temurin:21

LABEL org.opencontainers.image.authors="orx0815@users.noreply.github.com"

EXPOSE 8080

RUN groupadd --system sling && \
    useradd --no-log-init --system --gid sling sling && \
    mkdir /opt/sling && \
    mkdir /opt/sling/org.apache.sling.feature.launcher && \
    mkdir /opt/sling/launcher && \
    mkdir /opt/sling/artifacts && \
    mkdir /opt/sling/agents && \
    chown -R sling:sling /opt/sling/launcher

VOLUME /opt/sling/launcher

COPY src/main/container /opt/sling
COPY target/dependency/org.apache.sling.feature.launcher /opt/sling/org.apache.sling.feature.launcher
COPY target/artifacts /opt/sling/artifacts/

# ensure all files are readable by the sling user
# for some reason some jar files are 0600 while most are 0644
RUN find /opt/sling/artifacts -type f -perm 0600 | xargs --no-run-if-empty chmod 0644

USER sling:sling

WORKDIR /opt/sling
ENTRYPOINT [ "/opt/sling/bin/launch.sh" ]
CMD ["slingslop_aggregate"]