FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY target/*.jar notification_service.jar

#EXPOSE 8080

ENTRYPOINT ["java", "-jar", "notification_service.jar"]