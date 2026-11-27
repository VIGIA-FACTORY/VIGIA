// A deployable Spring Boot 4.1 service (ADR-002 v1.3). Applied by tools/service-template and every generated service.
plugins {
    id("vigia.java-conventions")
    id("org.springframework.boot")
}

// Deploy only the executable jar; the plain jar is not needed (05.01 §2.2, images by digest).
tasks.named<Jar>("jar") {
    enabled = false
}
