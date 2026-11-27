// Generates a new service under services/ from tools/service-template (05.01 §2.2, ADR-037 v1.3, QAS-26).
//   ./gradlew generateService -Pservice=s-01-community -Pstyle=hexagonal
// -Pstyle may be left out: the primary style of each service comes from ADR-037, and a different value is
// rejected so the code never drifts from the decision record.

/** The internal styles of ADR-037 v1.3 that the generator can create. */
enum class Style(val id: String) {
    HEXAGONAL("hexagonal"),
    LAYERED("layered"),
    EVENT_DRIVEN("event-driven");

    companion object {
        fun of(id: String): Style = entries.firstOrNull { it.id == id }
            ?: throw GradleException("Unknown style '$id'. Use one of: ${entries.joinToString { it.id }}")
    }
}

/** Primary style of each service, as recorded in ADR-037 v1.3. */
val stylesOfAdr037: Map<String, Style> = buildMap {
    listOf("01", "03", "04", "06", "07", "08", "13").forEach { put(it, Style.HEXAGONAL) }
    listOf("02", "09").forEach { put(it, Style.LAYERED) }
    listOf("05", "10", "11", "12").forEach { put(it, Style.EVENT_DRIVEN) }
}

/** A service folder name such as `s-03-access-control`, and the identifiers derived from it. */
class ServiceName(val folder: String) {
    private val match = PATTERN.matchEntire(folder)
        ?: throw GradleException("'$folder' is not a service name like s-01-community (05.01 §2.1)")

    val number: String = match.groupValues[1]
    private val contextWords: List<String> = match.groupValues[2].split("-")

    val javaPackage: String = "ec.edu.uce.vigia." + contextWords.joinToString("")
    val applicationClass: String = contextWords.joinToString("") { it.replaceFirstChar(Char::uppercase) } + "Application"
    val database: String = "vigia_" + contextWords.joinToString("_")
    val localHostPort: String = "81$number"

    companion object {
        val PATTERN = Regex("""s-(\d{2})-([a-z]+(?:-[a-z]+)*)""")
    }
}

/** Template values and what replaces them. Longer, more specific values go first. */
fun replacementsFor(service: ServiceName): List<Pair<String, String>> = listOf(
    "node ../gradle.mjs" to "node ../../tools/gradle.mjs",
    ":tools:service-template" to ":services:${service.folder}",
    "tools/service-template" to "services/${service.folder}",
    "service-template" to service.folder,
    "ec.edu.uce.vigia.template" to service.javaPackage,
    "ec/edu/uce/vigia/template" to service.javaPackage.replace('.', '/'),
    "TemplateApplication" to service.applicationClass,
    "vigia_template" to service.database,
    "8100:8080" to "${service.localHostPort}:8080",
)

fun String.replaceAll(replacements: List<Pair<String, String>>): String =
    replacements.fold(this) { text, (templateValue, serviceValue) -> text.replace(templateValue, serviceValue) }

/** Files of the template folder that belong to the generator, not to a service. */
val generatorOnlyFiles = setOf("generator.gradle.kts", "README.md")

/** Paths of the base template that only the hexagonal style keeps. */
val hexagonalOnlyPaths = listOf(
    "src/main/java/ec/edu/uce/vigia/template/domain/",
    "src/main/java/ec/edu/uce/vigia/template/application/",
    "src/main/java/ec/edu/uce/vigia/template/adapter/",
    "src/test/java/ec/edu/uce/vigia/template/HexagonalArchitectureTest.java",
)

tasks.register("generateService") {
    group = "vigia"
    description = "Creates services/<name> from tools/service-template. Usage: -Pservice=s-01-community [-Pstyle=hexagonal]"
    notCompatibleWithConfigurationCache("Writes new source folders chosen by command-line properties")

    doLast {
        val folder = providers.gradleProperty("service").orNull
            ?: throw GradleException("Missing -Pservice=<name>, for example -Pservice=s-01-community")
        val service = ServiceName(folder)
        val documentedStyle = stylesOfAdr037[service.number]
            ?: throw GradleException("S-${service.number} is not one of the services S-01 to S-13 (ADR-038)")
        val style = providers.gradleProperty("style").orNull?.let(Style::of) ?: documentedStyle
        if (style != documentedStyle) {
            throw GradleException(
                "ADR-037 gives S-${service.number} the style '${documentedStyle.id}', not '${style.id}'. " +
                    "Change ADR-037 first (new revision and 01.04 Change Log), then the generator.",
            )
        }

        val templateDir = file("tools/service-template")
        val targetDir = file("services/${service.folder}")
        if (targetDir.exists()) {
            throw GradleException("${targetDir.path} already exists; a service is generated only once")
        }
        val replacements = replacementsFor(service)

        fun copyTree(sourceRoot: File, skip: (String) -> Boolean) {
            sourceRoot.walkTopDown()
                .onEnter { dir -> dir.name != "build" && dir.name != ".gradle" && dir.name != "node_modules" }
                .filter { it.isFile }
                .forEach { source ->
                    val relativePath = source.relativeTo(sourceRoot).invariantSeparatorsPath
                    if (skip(relativePath)) return@forEach
                    val target = targetDir.resolve(relativePath.replaceAll(replacements))
                    target.parentFile.mkdirs()
                    target.writeText(source.readText().replaceAll(replacements))
                }
        }

        // 1. The common parts of the template (build, Dockerfile, Helm values, resources, health test).
        copyTree(templateDir) { path ->
            path in generatorOnlyFiles ||
                path.startsWith("styles/") ||
                (style != Style.HEXAGONAL && hexagonalOnlyPaths.any { path.startsWith(it) })
        }
        // 2. The package layout and the ArchUnit rules of the chosen style.
        if (style != Style.HEXAGONAL) {
            copyTree(templateDir.resolve("styles/${style.id}")) { false }
        }

        logger.lifecycle("Generated ${targetDir.path} with the ${style.id} style (ADR-037). Next:")
        logger.lifecycle("  pnpm install   (registers its package.json for Turborepo)")
        logger.lifecycle("  ./gradlew :services:${service.folder}:check")
        logger.lifecycle("  docker compose -f docker-compose.yml -f services/${service.folder}/local/compose.yaml up --build")
    }
}
