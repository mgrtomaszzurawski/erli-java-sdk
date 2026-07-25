// Root build. Holds shared coordinates and repositories; each module applies its own plugins.
// Quality gates are wired here for the hand-written modules; the generated erli-rest-models is exempt.

plugins {
    java
    alias(libs.plugins.spotless) apply false
}

allprojects {
    group = "io.github.mgrtomaszzurawski"

    repositories {
        mavenCentral()
    }

    // Generated Layer-1 sources carry Polish text from the spec descriptions; compile as UTF-8.
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// --- Spotless (formatting gate) -------------------------------------------------------------------
// Applied only to the hand-written modules. erli-rest-models is generated (openapi-generator) and is
// never hand-edited, so formatting it would be noise and its style is not ours to own.
val handWrittenModules = setOf("erli-client", "erli-demo", "erli-examples", "erli-jpms-consumer")
configure(subprojects.filter { it.name in handWrittenModules }) {
    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            // Conservative, non-reformatting steps: they fix real defects (dead imports, stray
            // whitespace, missing final newline, import order) without imposing a whole-file reformat
            // on carefully hand-laid code that uses 4-space indentation and deliberate wrapping.
            removeUnusedImports()
            importOrder()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}
