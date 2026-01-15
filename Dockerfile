# 1. 빌드 스테이지
FROM openjdk:17-jdk-slim
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
# 2. 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]