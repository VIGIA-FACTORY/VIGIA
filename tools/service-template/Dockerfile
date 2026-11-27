# Built from the repository root so the service can use libs/ and the Gradle wrapper:
#   docker build -f tools/service-template/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon :tools:service-template:bootJar \
    && cp tools/service-template/build/libs/*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system vigia && useradd --system --gid vigia vigia
WORKDIR /app
COPY --from=build /workspace/application.jar application.jar
USER vigia
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
