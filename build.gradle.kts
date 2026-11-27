// Shared configuration lives in the convention plugins of tools/build-logic (05.01 §2.2).
// The root project only registers the service generator.
apply(from = "tools/service-template/generator.gradle.kts")
