FROM maven:3.6.3-jdk-11-slim as build-hapi
WORKDIR /tmp/hapi-fhir-jpaserver-starter

COPY pom.xml .
RUN mvn -ntp dependency:go-offline

COPY src/ /tmp/hapi-fhir-jpaserver-starter/src/
RUN mvn clean package spring-boot:repackage -Pboot


#FINAL IMAGE
FROM gcr.io/distroless/java:11

COPY --from=build-hapi /tmp/hapi-fhir-jpaserver-starter/target/hapi.war /app/hapi.war

EXPOSE 8080

WORKDIR /app

CMD ["hapi.war"] 
