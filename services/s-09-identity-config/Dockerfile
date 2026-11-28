# Built from the repository root so the service can use libs/ and the Gradle wrapper:
#   docker build -f services/s-09-identity-config/Dockerfile .
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon :services:s-09-identity-config:bootJar \
    && cp services/s-09-identity-config/build/libs/*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system vigia && useradd --system --gid vigia vigia
WORKDIR /app
COPY --from=build /workspace/application.jar application.jar
USER vigia
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "application.jar"]
