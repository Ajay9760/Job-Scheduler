# Build context is the REPOSITORY ROOT (see docker-compose.yml): the module needs chronos-common.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY chronos-common/pom.xml chronos-common/
COPY api-service/pom.xml api-service/
COPY scheduler-service/pom.xml scheduler-service/
COPY worker-service/pom.xml worker-service/
RUN mvn -q -B -pl worker-service -am dependency:go-offline || true
COPY chronos-common chronos-common
COPY worker-service worker-service
RUN mvn -q -B -pl worker-service -am -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 chronos
WORKDIR /app
COPY --from=build /src/worker-service/target/worker-service-*-SNAPSHOT.jar app.jar
USER chronos
EXPOSE 8082
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
