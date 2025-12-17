# ---------- Build stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy root POM
COPY pom.xml .

# Copy module POMs
COPY api-service/pom.xml api-service/pom.xml
COPY scheduler-service/pom.xml scheduler-service/pom.xml
COPY worker-service/pom.xml worker-service/pom.xml

# Download dependencies (fast, cacheable)
RUN mvn -B -pl api-service -am dependency:go-offline

# Copy module sources
COPY api-service/src api-service/src

# Build only api-service + required parents
RUN mvn -B -pl api-service -am clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY target/api-service-1.0.0-SNAPSHOT-exec.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
