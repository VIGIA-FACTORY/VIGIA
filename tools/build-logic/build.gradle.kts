// Convention plugins shared by every Java module of the monorepo (DP-06: one source for build rules).
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot.gradle.plugin)
}
