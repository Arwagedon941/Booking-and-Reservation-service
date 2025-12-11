FROM eclipse-temurin:17-jre
ARG JAR_FILE=target/data-processor-0.1.0-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} app.jar
EXPOSE 8083
ENTRYPOINT ["java","-jar","/app/app.jar"]




