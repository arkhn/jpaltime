FROM maven:3.6.3-jdk-11-slim as build-hapi
WORKDIR /tmp/hapi-fhir-jpaserver-starter

COPY pom.xml .
COPY server.xml .
RUN mvn -ntp dependency:go-offline

COPY src/ /tmp/hapi-fhir-jpaserver-starter/src/
RUN mvn clean install -DskipTests

FROM build-hapi AS build-distroless
RUN mvn package spring-boot:repackage -Pboot
RUN mkdir /app && \
    cp /tmp/hapi-fhir-jpaserver-starter/target/ROOT.war /app/main.war

FROM alpine:3.13 as hapi-fhir-cli

WORKDIR /tmp

RUN apk add wget tar
RUN wget -4 --no-verbose https://github.com/hapifhir/hapi-fhir/releases/download/v5.3.0/hapi-fhir-5.3.0-cli.tar.bz2
RUN tar xjf hapi-fhir-5.3.0-cli.tar.bz2

#FINAL IMAGE
FROM gcr.io/distroless/java-debian10:11 AS release-distroless
COPY --chown=nonroot:nonroot --from=build-distroless /app /app
COPY --from=hapi-fhir-cli /tmp/hapi-fhir-cli.jar /app/hapi-fhir-cli.jar

EXPOSE 8080

# 65532 is the nonroot user's uid
# used here instead of the name to allow Kubernetes to easily detect that the container
# is running as a non-root (uid != 0) user.
USER 65532:65532

WORKDIR /app

CMD ["/app/main.war"]