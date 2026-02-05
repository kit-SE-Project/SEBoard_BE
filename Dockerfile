FROM openjdk:17-jdk-slim-buster
COPY ./seboard-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources resources
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]