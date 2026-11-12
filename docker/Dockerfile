# Local image only. Do not push this to a Frontiers registry and do not bake
# keycloak.json into the image. docker/.dockerignore excludes that file.
# https://hub.docker.com/r/sonatype/nexus3
FROM sonatype/nexus3:3.69.0

ENV NEXUS_PLUGINS=${NEXUS_HOME}/system
ARG KEYCLOAK_PLUGIN_VERSION=0.6.0-nexus3.69.0
ENV KEYCLOAK_PLUGIN=/org/github/flytreeleft/nexus3-keycloak-plugin/${KEYCLOAK_PLUGIN_VERSION}/nexus3-keycloak-plugin-${KEYCLOAK_PLUGIN_VERSION}

USER root

COPY nexus3-keycloak-plugin-${KEYCLOAK_PLUGIN_VERSION}.jar ${NEXUS_PLUGINS}${KEYCLOAK_PLUGIN}.jar

RUN chmod 644 ${NEXUS_PLUGINS}${KEYCLOAK_PLUGIN}.jar \
    && echo "reference\\:file\\:${KEYCLOAK_PLUGIN}.jar = 200" >> ${NEXUS_HOME}/etc/karaf/startup.properties \
    && chown -R nexus:nexus /opt/sonatype/nexus

USER nexus
