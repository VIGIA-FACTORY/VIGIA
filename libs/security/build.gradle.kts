plugins {
    id("vigia.java-library-conventions")
}

dependencies {
    // Standard OAuth2 resource server only: issuer-uri and JWKS, no Keycloak library (ADR-050). Each service
    // that validates tokens adds the starter itself, so the library does not switch security on everywhere.
    compileOnly(libs.spring.boot.starter.oauth2.resource.server)
    testImplementation(libs.spring.boot.starter.oauth2.resource.server)

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
