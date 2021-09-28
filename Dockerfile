FROM maven:3.6.3-jdk-11-slim as build-hapi
WORKDIR /tmp/hapi-fhir-jpaserver-starter

COPY pom.xml .
COPY server.xml .
RUN mvn -ntp dependency:go-offline

COPY src/ /tmp/hapi-fhir-jpaserver-starter/src/
RUN mvn clean package spring-boot:repackage -Pboot

FROM alpine:3.13 as hapi-fhir-cli

WORKDIR /tmp

RUN apk add wget tar
RUN wget -4 --no-verbose https://github.com/hapifhir/hapi-fhir/releases/download/v5.5.0/hapi-fhir-5.5.0-cli.tar.bz2
RUN tar xjf hapi-fhir-5.5.0-cli.tar.bz2

#FINAL IMAGE
FROM gcr.io/distroless/java:11

COPY --from=build-hapi /tmp/hapi-fhir-jpaserver-starter/target/hapi.war /app/hapi.war
COPY --from=hapi-fhir-cli /tmp/hapi-fhir-cli.jar /app/hapi-fhir-cli.jar

COPY catalina.properties /usr/local/tomcat/conf/catalina.properties
COPY server.xml /usr/local/tomcat/conf/server.xml

WORKDIR /app

CMD ["hapi.war"] 
