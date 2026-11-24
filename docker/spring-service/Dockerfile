# syntax=docker/dockerfile:1
# Shared image for every Spring Boot service. Build context is the repo root;
# pick the service with the SERVICE build arg (e.g. SERVICE=farmer-service).

FROM maven:3.9-eclipse-temurin-21 AS build
ARG SERVICE
WORKDIR /build
COPY ${SERVICE}/pom.xml .
COPY ${SERVICE}/src ./src
# Tests are skipped here: the @SpringBootTest context tests need MySQL. Run them with mvnw outside Docker.
# The .m2 cache mount is shared by all services, so dependencies download once.
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
