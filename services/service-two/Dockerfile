FROM eclipse-temurin:17-jre
ARG JAR_FILE=target/service-two-0.1.0-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]


