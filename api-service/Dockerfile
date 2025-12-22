FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY scheduler-service/pom.xml scheduler-service/pom.xml

RUN mvn -B -pl scheduler-service dependency:go-offline

COPY scheduler-service/src scheduler-service/src

RUN mvn -B -pl scheduler-service clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/scheduler-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]