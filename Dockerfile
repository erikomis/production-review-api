FROM ubuntu:latest AS build

RUN apt-get update
RUN apt-get install openjdk-17-jdk -y
COPY . .

RUN apt-get install maven -y
RUN mvn clean install



FROM eclipse-temurin:17

WORKDIR /app
RUN mvn clean package
COPY target/production-review-0.0.1-SNAPSHOT.jar /app/production-review-0.0.1-SNAPSHOT.jar

ENV TZ=America/Sao_Paulo

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/production-review-0.0.1-SNAPSHOT.jar"]

