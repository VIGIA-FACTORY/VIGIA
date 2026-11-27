// A deployable Spring Boot 4.1 service (ADR-002 v1.3). Applied by tools/service-template and every generated service.
plugins {
    id("vigia.java-conventions")
    id("org.springframework.boot")
    id("com.google.cloud.tools.jib")
}

// Deploy only the executable jar; the plain jar is not needed (05.01 §2.2, images by digest).
tasks.named<Jar>("jar") {
    enabled = false
}

// Stage 9 of 05.01 §3.2: the image is built with Jib (no Docker daemon needed in CI), tagged with the commit SHA,
// pushed to GHCR and then signed with cosign by digest. "latest" is never used.
jib {
    from {
        image = "eclipse-temurin:21-jre"
    }
    to {
        image = "ghcr.io/vigia-factory/${project.name}"
        tags = setOf(providers.environmentVariable("GITHUB_SHA").getOrElse("local"))
    }
    container {
        user = "1000:1000"
        ports = listOf("8080")
        jvmFlags = listOf("-XX:MaxRAMPercentage=75")
    }
}

// Jib 3.5 reads the project at execution time, which the configuration cache rejects; its tasks run without it.
tasks.matching { it.name.startsWith("jib") }.configureEach {
    notCompatibleWithConfigurationCache("Jib 3.5 uses Task.project at execution time")
}
