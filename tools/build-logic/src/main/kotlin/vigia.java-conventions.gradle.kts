// Base rules for every Java module (05.01 §3.2): Java 21 toolchain (03.04.01 §11), the Spring Boot and
// Testcontainers BOMs, formatting with Spotless and Checkstyle (stage 1), unit and architecture tests with
// JaCoCo coverage of the domain (stage 2) and integration tests tagged "integration" (stage 8).
import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    java
    checkstyle
    jacoco
    id("com.diffplug.spotless")
}

val libs = the<VersionCatalogsExtension>().named("libs")
fun version(alias: String): String = libs.findVersion(alias).get().requiredVersion

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

// Exact resolved versions are locked in gradle.lockfile, so builds are reproducible and Trivy can scan the
// Java dependencies in stage 6. Refresh after a dependency change: ./gradlew resolveAndLockAll --write-locks
dependencyLocking {
    lockAllConfigurations()
}

tasks.register("resolveAndLockAll") {
    description = "Resolves every configuration; run with --write-locks to refresh gradle.lockfile."
    group = "help"
    notCompatibleWithConfigurationCache("Resolves configurations at execution time")
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) { "Run with --write-locks" }
    }
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

// Stage 1: formatting (Spotless with palantir-java-format, 4-space indent) and style rules (Checkstyle).
spotless {
    java {
        palantirJavaFormat(version("palantir-java-format"))
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

checkstyle {
    toolVersion = version("checkstyle")
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

// Stage 2: unit and architecture tests. Tests tagged "integration" run in their own task (stage 8).
tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("integration") }
}

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs the tests tagged 'integration' (Testcontainers, LocalStack), stage 8 of 05.01 §3.2."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    shouldRunAfter(tasks.named("test"))
}

jacoco {
    toolVersion = version("jacoco")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required = true
        html.required = true
    }
}

// At least 80 % line coverage in each service's domain package (05.03 §1.1).
tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    violationRules {
        rule {
            element = "PACKAGE"
            includes = listOf("*.domain", "*.domain.*")
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(integrationTest, tasks.named("jacocoTestReport"), tasks.named("jacocoTestCoverageVerification"))
}
