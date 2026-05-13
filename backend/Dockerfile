FROM maven:3.9.12-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src

RUN mvn clean package -DskipTests


FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar catalog_service.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "catalog_service.jar"]
