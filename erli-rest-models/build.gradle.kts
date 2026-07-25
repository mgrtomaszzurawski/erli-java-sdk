// Layer 1 — transport models generated from openapi/swagger.json (Jackson "native" library).
// Generated sources are NOT committed; the spec is source-of-truth and never hand-edited.
// Only *Raw transport POJOs live here; they are never exported to consumers (JPMS-internal).

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
    `java-library`
    alias(libs.plugins.openapi.generator)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.databind.nullable)
    compileOnly(libs.jakarta.annotation.api)
}

val vendoredSpec = rootProject.file("openapi/swagger.json")
val normalizedSpec = layout.buildDirectory.file("spec/swagger.normalized.json")
val generatedRoot = layout.buildDirectory.dir("generated/openapi")

// The vendored spec is upstream's and is never hand-edited. A few polymorphic oneOf/anyOf "value"
// schemas include an inline array branch, which trips a known openapi-generator bug (it emits an
// invalid `List<X>.class` token). This step writes a build-only normalized copy that collapses
// exactly those array-branch composites to free-form objects (Object at Layer 1; the domain layer
// re-types them). The vendored spec stays pristine. See ADR/ADR-001-generate-from-spec-models-only.md.
val normalizeSpec by tasks.registering {
    inputs.file(vendoredSpec)
    outputs.file(normalizedSpec)
    doLast {
        val root = JsonSlurper().parse(vendoredSpec)

        fun isArrayBranch(branch: Any?): Boolean =
            branch is Map<*, *> && (branch["type"] == "array" || branch.containsKey("items"))

        fun scrub(node: Any?) {
            when (node) {
                is MutableMap<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    val map = node as MutableMap<String, Any?>
                    val composedKey = listOf("oneOf", "anyOf").firstOrNull { map[it] is List<*> }
                    val branches = composedKey?.let { map[it] as List<*> }
                    if (branches != null && branches.any { isArrayBranch(it) }) {
                        val description = map["description"]
                        map.clear()
                        if (description != null) {
                            map["description"] = description
                        }
                        return
                    }
                    // Array item enums are dropped at Layer 1: the generator's dead-code
                    // toUrlQueryString helper mis-types `List<ItemEnum>` as `List<String>` and fails
                    // to compile. Layer 1 is internal *Raw; the domain layer re-applies typed enums.
                    val items = map["items"]
                    if (map["type"] == "array" && items is MutableMap<*, *> && items.containsKey("enum")) {
                        @Suppress("UNCHECKED_CAST")
                        (items as MutableMap<String, Any?>).remove("enum")
                    }
                    map.values.toList().forEach { scrub(it) }
                }
                is List<*> -> node.forEach { scrub(it) }
            }
        }

        scrub(root)
        val target = normalizedSpec.get().asFile
        target.parentFile.mkdirs()
        target.writeText(JsonOutput.toJson(root))
    }
}

openApiGenerate {
    generatorName = "java"
    inputSpec = normalizedSpec.get().asFile.toString()
    outputDir = generatedRoot.get().asFile.toString()

    // Upstream spec has minor validation gaps (path params lacking required:true, stray
    // examples/default attributes). We never hand-edit the spec, so validation is skipped here and
    // covered by tests + the live wire instead. See ADR/ADR-001-generate-from-spec-models-only.md.
    validateSpec.set(false)
    skipValidateSpec.set(true)

    modelPackage = "io.github.mgrtomaszzurawski.erli.rest.model"
    apiPackage = "io.github.mgrtomaszzurawski.erli.rest.api"
    invokerPackage = "io.github.mgrtomaszzurawski.erli.rest.invoker"

    // Layer 1 is models only; the SDK reimplements transport itself (no generated api client).
    generateApiTests = false
    generateApiDocumentation = false
    generateModelTests = false
    generateModelDocumentation = false
    // Selective generation: models + their supporting runtime (the invoker package: JSON helper,
    // AbstractOpenApiSchema, date formats). NO per-endpoint api-client classes — the SDK reimplements
    // transport itself (ADR-002). Listing "models"+"supportingFiles" but not "apis" excludes apis.
    globalProperties = mapOf(
        "models" to "",
        "supportingFiles" to "",
        "modelDocs" to "false",
    )

    configOptions = mapOf(
        "library" to "native",
        "useJakartaEe" to "true",
        "openApiNullable" to "true",
        "serializationLibrary" to "jackson",
        "hideGenerationTimestamp" to "true",
        "sourceFolder" to "src/main/java",
    )
}

tasks.named("openApiGenerate") {
    dependsOn(normalizeSpec)
}

sourceSets {
    named("main") {
        java.srcDir(generatedRoot.map { it.dir("src/main/java") })
    }
}

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

// Layer 1 ships without a module-info (generated POJOs), so it is consumed as an automatic module.
// Pin its name so erli-client's module-info can `requires` it deterministically instead of relying
// on the jar-filename derivation. The package is still never re-exported (JPMS-internal).
tasks.named<Jar>("jar") {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.mgrtomaszzurawski.erli.rest.models")
    }
}
