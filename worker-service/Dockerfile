# ---------- Build stage ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy parent POM
COPY pom.xml .

# Copy module POMs
COPY api-service/pom.xml api-service/pom.xml
COPY scheduler-service/pom.xml scheduler-service/pom.xml
COPY worker-service/pom.xml worker-service/pom.xml

# Download dependencies
RUN mvn -B -pl worker-service -am dependency:go-offline

# Copy source code
COPY worker-service/src worker-service/src
COPY api-service/src api-service/src

# Build worker-service
RUN mvn -B -pl worker-service -am clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/worker-service/target/*-exec.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
