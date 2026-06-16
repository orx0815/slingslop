# Adopted from https://github.com/apache/sling-org-apache-sling-starter/blob/master/Dockerfile

FROM docker.io/eclipse-temurin:25

LABEL org.opencontainers.image.authors="orx0815@users.noreply.github.com"

EXPOSE 8080

RUN groupadd --system sling && \
    useradd --no-log-init --system --gid sling sling && \
    mkdir /opt/sling && \
    mkdir /opt/sling/org.apache.sling.feature.launcher && \
    mkdir /opt/sling/launcher && \
    mkdir /opt/sling/artifacts && \
    mkdir /opt/sling/agents && \
    mkdir /opt/sling/seed-repository && \
    chown -R sling:sling /opt/sling/launcher && \
    chown -R sling:sling /opt/sling/seed-repository

# /opt/sling/launcher is the writable working directory of the feature launcher
# and holds the read-write SegmentNodeStore (segment store of the
# 'composite-global' mount when running in composite mode).
VOLUME /opt/sling/launcher

# /opt/sling/seed-repository holds the read-only SegmentNodeStore for /apps
# and /libs when running in composite mode. It is intentionally NOT a VOLUME:
# the immutable application part is baked into the image and replaced by
# rolling forward to a new image. The base image ships this directory empty;
# Dockerfile.composite copies a pre-seeded segmentstore into it.
# See docs/composite-nodestore.md.

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