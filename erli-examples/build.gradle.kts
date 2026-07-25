// erli-examples — compile-only consumer snippets that double as documentation. NOT published.
// If a snippet stops compiling, the public API changed in a breaking way — that is the point.
// Classpath consumer (no module-info); the modular view is covered by erli-jpms-consumer.

plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(project(":erli-client"))
}
