# ---------- Build stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy parent POM
COPY pom.xml .

# Copy module POMs (required for Maven reactor)
COPY api-service/pom.xml api-service/pom.xml
COPY scheduler-service/pom.xml scheduler-service/pom.xml
COPY worker-service/pom.xml worker-service/pom.xml

# Download dependencies
RUN mvn -B -pl scheduler-service -am dependency:go-offline

# Copy source code
COPY scheduler-service/src scheduler-service/src
COPY api-service/src api-service/src

# Build scheduler-service
RUN mvn -B clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app


COPY target/scheduler-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.jar"]
