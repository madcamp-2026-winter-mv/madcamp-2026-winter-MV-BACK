# 1. Java 21이 설치된 공식 OpenJDK 이미지를 베이스로 사용
FROM openjdk:21

# 2. build/libs 폴더 아래 있는 .jar 파일을 JAR_FILE이라는 변수로 정의
ARG JAR_FILE=build/libs/*.jar

# 3. 위에서 정의한 JAR 파일을 app.jar 라는 이름으로 컨테이너에 복사
COPY ${JAR_FILE} app.jar

# 4. 컨테이너 실행 시 'java -jar /app.jar' 명령을 자동으로 실행하도록 지정
ENTRYPOINT ["java", "-jar", "/app.jar"]