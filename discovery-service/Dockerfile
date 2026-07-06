FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY target/*.jar discovery-service.jar

#EXPOSE 8080

ENTRYPOINT ["java", "-jar", "discovery-service.jar"]