plugins {
    id("vigia.java-library-conventions")
}

dependencies {
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.keycloak)
}

// The realm as code (infra/identity/realm, ADR-050) is on the test classpath, so the integration test
// imports exactly the file that docker compose and the cluster use.
sourceSets {
    test {
        resources.srcDir(rootProject.file("infra/identity/realm"))
    }
}
