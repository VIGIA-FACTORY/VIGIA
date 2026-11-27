pluginManagement {
    includeBuild("tools/build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "vigia"

// Shared Java code (05.01 §2.1). A service depends only on libs/ and generated contracts (DP-09).
include(":libs:sdk-java", ":libs:security")

// The service template is a working service, built and tested with the rest so it never rots (05.01 §2.2).
include(":tools:service-template")

// Every folder under services/ with a build file is a service subproject (05.01 §2.1).
file("services")
    .listFiles { candidate -> candidate.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { serviceDir -> include(":services:${serviceDir.name}") }
