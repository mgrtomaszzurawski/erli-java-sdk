// erli-client — the published SDK. Holds the three hand-written layers:
//   sdk.core     (public)  — auth, errors, retry, shared value/ID types
//   internal     (hidden)  — HttpRuntime over java.net.http, JSON codec, error mapping, pagination
//   sdk.domain.* (public)  — typed facades; consumers import only these + sdk.core
// Layer 1 (erli-rest-models) and Jackson are `implementation` deps: they are transport details and
// must never appear in an exported signature (the JPMS-consumer module enforces this). See ADR-002.

plugins {
    `java-library`
    jacoco
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

jacoco {
    toolVersion = "0.8.12"
}

dependencies {
    // Layer 1 raw models + Jackson stay internal to this module (never re-exported to consumers).
    implementation(project(":erli-rest-models"))
    implementation(libs.jackson.databind)
    // Declared directly because JsonCodec imports JavaTimeModule and module-info `requires` it. It also
    // arrives transitively through erli-rest-models' `api`, but relying on that would break this module
    // the day Layer 1 narrows that dependency — and the JPMS gate would not catch it, because the
    // consumer's compile classpath never resolves erli-client's transitive requires.
    implementation(libs.jackson.datatype.jsr310)
    // Same reasoning for the nullable module: `nullable: true` properties are generated as
    // JsonNullable<T>, and JsonCodec registers the module that decodes them.
    implementation(libs.jackson.databind.nullable)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    // WireMock drives HttpRuntime tests against a real local HTTP server (verify-on-write). See TESTING.md.
    testImplementation(libs.wiremock.standalone)
}

tasks.test {
    // Unit tests only. Live @Tag("e2e") tests hit the sandbox and run via the e2eTest task below.
    useJUnitPlatform {
        excludeTags("e2e")
    }
    finalizedBy(tasks.jacocoTestReport)
}

// Line/branch coverage of the hand-written SDK. The generated Layer 1 (erli-rest-models) is a
// separate module and carries no JaCoCo; module-info has no executable coverage, so it is excluded.
// Report-only for now (does not fail the build) — a coverage floor can be ratcheted in once the
// baseline is documented in the binding CLAUDE.md.
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        classDirectories.files.map { classDir ->
            fileTree(classDir) { exclude("module-info.class") }
        }
    )
}

// Live end-to-end tests against the Erli sandbox. Reads ERLI_BASE_URL / ERLI_API_KEY from the
// environment (sourced from /workspace/shared/secrets/erli-sandbox.env); each e2e test assumes those
// are present and self-skips otherwise. Run: `./gradlew :erli-client:e2eTest`.
tasks.register<Test>("e2eTest") {
    description = "Runs @Tag(\"e2e\") live-sandbox tests (requires ERLI_BASE_URL + ERLI_API_KEY)."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("e2e")
    }
    // Always re-run: results depend on live server state, not just inputs.
    outputs.upToDateWhen { false }
}

// PIT mutation testing — the adequacy oracle (are the tests load-bearing, not faked?). Configured in
// this module (not the root) because the pitest plugin registers its extension only once the java
// plugin is present, which is here. Report-only, run explicitly at develop->main / release, NOT
// per-PR (slow; equivalent mutants make 100% impossible). Targets the business logic; the @Tag("e2e")
// live tests are excluded so no minion hits the network. Band ~70-85%. Run: ./gradlew :erli-client:pitest
apply(plugin = "info.solidsoft.pitest")
configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
    pitestVersion.set("1.17.0")
    junit5PluginVersion.set("1.2.1")
    targetClasses.set(
        listOf(
            "io.github.mgrtomaszzurawski.erli.internal.*",
            "io.github.mgrtomaszzurawski.erli.core.retry.*",
            "io.github.mgrtomaszzurawski.erli.core.model.*",
            "io.github.mgrtomaszzurawski.erli.core.error.*",
        ),
    )
    excludedGroups.set(listOf("e2e")) // JUnit5 tag: keep live-sandbox tests out of the minions
    threads.set(6) // ~0.5 GB/minion, under the 8 GB cgroup cap
    timestampedReports.set(false)
    outputFormats.set(listOf("XML", "HTML"))
    // Report-only: no mutationThreshold, so it never fails the build.
}
