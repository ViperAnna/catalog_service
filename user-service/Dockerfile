FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY target/*.jar user_service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "user_service.jar"]