FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY target/*.jar seller_service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "seller_service.jar"]