// erli-client — the published SDK. Holds the three hand-written layers:
//   sdk.core     (public)  — auth, errors, retry, shared value/ID types
//   internal     (hidden)  — HttpRuntime over java.net.http, JSON codec, error mapping, pagination
//   sdk.domain.* (public)  — typed facades; consumers import only these + sdk.core
// Layer 1 (erli-rest-models) and Jackson are `implementation` deps: they are transport details and
// must never appear in an exported signature (the JPMS-consumer module enforces this). See ADR-002.

plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
}

dependencies {
    // Layer 1 raw models + Jackson stay internal to this module (never re-exported to consumers).
    implementation(project(":erli-rest-models"))
    implementation(libs.jackson.databind)

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
