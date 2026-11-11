FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/server-allday-backend-0.1.0.jar /app/app.jar
USER 10001:10001
EXPOSE 18080
ENTRYPOINT ["java","-jar","/app/app.jar"]
