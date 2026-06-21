FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
COPY gradlew gradlew.bat ./
COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /home/proxy
RUN adduser -D -u 10000 proxy
COPY --from=build /app/build/libs/*.jar ./mtproto-proxy.jar
USER proxy
CMD ["java", "-jar", "mtproto-proxy.jar"]
