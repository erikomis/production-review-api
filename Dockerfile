FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# baixa as dependências em uma camada separada para aproveitar o cache do Docker
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre

ENV TZ=America/Sao_Paulo

# as configurações (banco, SECRET, MinIO, e-mail...) vêm do .env via docker-compose
RUN useradd --system --uid 1001 app
USER app

COPY --from=build /app/target/*.jar /app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app.jar"]
