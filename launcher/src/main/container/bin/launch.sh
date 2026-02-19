#!/bin/bash -e

feature_name="${1}"
feature=$(find artifacts -name "*${feature_name}*.slingosgifeature")

if [[ ! -f "${feature}" ]]; then
    echo "[ERROR] Did not find any feature file matching name ${feature_name}. Aborting"
    exit 1
fi

docker_feature=$(find artifacts -name "*docker.slingosgifeature")

echo "[INFO] Selected ${feature} for launching"
echo "[INFO] Automatically appended ${docker_feature}"

feature="${feature},${docker_feature}"

if [ ! -z "${JAVA_DEBUG_PORT}" ]; then
    JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${JAVA_DEBUG_PORT}"
fi
# remove add-opens after SLING-10831 is fixed
JAVA_OPTS="--add-opens java.base/java.lang=ALL-UNNAMED ${JAVA_DEBUG_OPTS} ${EXTRA_JAVA_OPTS}"

agents=$(find agents -name "*.jar")
for agent in ${agents}; do
    echo "[INFO] Discovered agent ${agent}"
    JAVA_OPTS="-javaagent:${agent} ${JAVA_OPTS}"
done

export JAVA_OPTS
echo "[INFO] JAVA_OPTS=${JAVA_OPTS}"

exec org.apache.sling.feature.launcher/bin/launcher \
    -c artifacts \
    -CC "org.apache.sling.commons.log.LogManager=MERGE_LATEST" \
    -f ${feature}