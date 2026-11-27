# services

One Gradle subproject per microservice, S-01 to S-13 (05.01 §2.1, ADR-038), on Spring Boot 4.1 and Java 21.
Every service is generated from `tools/service-template` with its ADR-037 style:

```
./gradlew generateService -Pservice=s-01-community -Pstyle=hexagonal
```

A service depends only on `libs/` and the generated contracts, never on another service (DP-09).
