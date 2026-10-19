FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY target/*.jar order_service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "order_service.jar"]
