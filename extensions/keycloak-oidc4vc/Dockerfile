FROM sbtscala/scala-sbt:graalvm-ce-22.3.3-b1-java17_1.9.8_3.3.1 AS builder
RUN mkdir -p /workspace
WORKDIR /workspace
COPY build.sbt /workspace/build.sbt
COPY project/build.properties /workspace/project/build.properties
RUN sbt update
COPY . /workspace
RUN sbt package

FROM quay.io/keycloak/keycloak:23.0.7
COPY --from=builder ./workspace/target/keycloak-oea-oidc-0.1.0-SNAPSHOT.jar /opt/keycloak/providers/keycloak-oea-oidc-0.1.0-SNAPSHOT.jar
RUN /opt/keycloak/bin/kc.sh build
ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]
