FROM eclipse-temurin:21-jre-alpine
COPY target/ERP_SpringBoot-Experiment-0.0.1-SNAPSHOT.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]