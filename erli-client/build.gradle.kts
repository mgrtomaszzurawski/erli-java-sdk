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
    useJUnitPlatform()
}
