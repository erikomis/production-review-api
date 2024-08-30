FROM maven:3.8.3-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17

COPY --from=build /app/target/*.jar app.jar

ENV TZ=America/Sao_Paulo
ENV MAIL_USERNAME=TESTE
ENV MAIL_PASSWORD=TESTE
ENV SECRET=TESTE

ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/TESTE
ENV SPRING_DATASOURCE_USERNAME=TESTE
ENV SPRING_DATASOURCE_PASSWORD=TESTE

ENV URL=TESTE
ENV ACESS_NAME=TESTE
ENV ACESS_SECREY=TESTE

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app.jar"]