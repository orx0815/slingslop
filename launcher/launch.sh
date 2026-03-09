#!/bin/sh -eu
echo '-------------------------------------------------------------------------------------------'
echo '[NOTE] Launching application, this will fail if you did not build the project at least once'
echo '[NOTE] Remove the launcher folder to throw away local changes'
echo '-------------------------------------------------------------------------------------------'

export JAVA_OPTS="-XX:+UseG1GC -XX:+UseCompactObjectHeaders -XX:+UseStringDeduplication \
                  -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=38080"

target/dependency/org.apache.sling.feature.launcher/bin/launcher -f target/slingfeature-tmp/feature-slingslop_aggregate.json
