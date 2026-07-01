FROM eclipse-temurin:21-jdk AS builder
LABEL authors="p2vman"
WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle

COPY . .

RUN chmod +x ./gradlew \
    && ./gradlew clean shadowJar --no-daemon

FROM eclipse-temurin:21-jre
LABEL authors="p2vman"
WORKDIR /app

COPY --from=builder /app/build/libs/server.jar /app/server.jar

RUN mkdir -p /app/logs

VOLUME ["/app/logs", "/app"]

ENTRYPOINT ["java", "-jar", "/app/server.jar"]