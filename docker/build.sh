#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

IMAGE_NAME=nexus-oss/nexus3
IMAGE_VERSION=3.69.0-keycloak
PLUGIN_VERSION=0.6.0-nexus3.69.0

cp "${DIR}/../target/nexus3-keycloak-plugin-${PLUGIN_VERSION}.jar" "${DIR}/"

docker build -t ${IMAGE_NAME}:${IMAGE_VERSION} \
             -f "${DIR}/Dockerfile" "${DIR}"
