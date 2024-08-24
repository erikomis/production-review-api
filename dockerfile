FROM eclipse-temurin:17

WORKDIR /app

COPY target/production-review-0.0.1-SNAPSHOT.jar /app/production-review-0.0.1-SNAPSHOT.jar

ENV TZ=America/Sao_Paulo

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/production-review-0.0.1-SNAPSHOT.jar"]
