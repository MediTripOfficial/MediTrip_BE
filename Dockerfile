FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

COPY build/libs/*-SNAPSHOT.jar app.jar

ENV TZ=Asia/Seoul
EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
