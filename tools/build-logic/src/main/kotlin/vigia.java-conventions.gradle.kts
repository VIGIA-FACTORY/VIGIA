// Base rules for every Java module: Java 21 toolchain (03.04.01 §11), JUnit 5, UTF-8, the Spring Boot BOM
// and the Testcontainers BOM for integration tests (05.03 §1.1).
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
}

val libs = the<VersionCatalogsExtension>().named("libs")

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    testImplementation(platform(SpringBootPlugin.BOM_COORDINATES))
    testImplementation(platform(libs.findLibrary("testcontainers-bom").get()))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
