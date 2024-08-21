FROM eclipse-temurin:17

WORKDIR /app


COPY target/seu-app.jar /app/seu-app.jar


ENV TZ=America/Sao_Paulo


EXPOSE 8084


ENTRYPOINT ["java", "-jar", "/app/seu-app.jar"]