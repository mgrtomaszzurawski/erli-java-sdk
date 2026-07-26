// Root build. Holds shared coordinates and repositories; each module applies its own plugins.
// Quality gates are wired here for the hand-written modules; the generated erli-rest-models is exempt.

plugins {
    java
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.pitest) apply false
    alias(libs.plugins.sonarqube)
}

// SonarQube analysis (quality platform). Config only — host/credentials come from the environment at
// invocation, never the build file (they are secrets). Not part of `build`; run explicitly at PR-ready:
//   ./gradlew sonar --no-configuration-cache \
//     -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_LOGIN -Dsonar.password=$SONAR_PASSWORD
// after a full `build` so the JaCoCo XML exists (else new_coverage reads 0).
sonar {
    properties {
        property("sonar.projectKey", "erli-java-sdk")
        property("sonar.projectName", "erli-java-sdk")
        property("sonar.sourceEncoding", "UTF-8")
        // Coverage is measured only on the hand-written SDK module.
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${rootDir}/erli-client/build/reports/jacoco/test/jacocoTestReport.xml",
        )
    }
}

// The generated Layer-1 module is not our code and is never hand-edited — keep it out of the analysis.
project(":erli-rest-models") {
    sonar {
        isSkipProject = true
    }
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

    apply(plugin = "checkstyle")
    configure<CheckstyleExtension> {
        toolVersion = "10.21.1"
        configDirectory.set(rootProject.file("config/checkstyle"))
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false // the tree is clean; a new violation now fails the build
        isShowViolations = true
        maxWarnings = 0
    }
    tasks.withType<Checkstyle>().configureEach {
        // module-info.java is a declarative descriptor, not styled code, and Checkstyle's parser
        // throws on its grammar (requires/exports). Nothing in the config applies to it anyway.
        exclude("**/module-info.java")
        reports {
            xml.required.set(true)
            html.required.set(false)
        }
    }

    // PMD's stricter rules (complexity, System.out) target the shipped SDK, not the illustrative
    // demo/examples/jpms modules (whose println and identity checks are correct there).
    if (name == "erli-client") {
        apply(plugin = "pmd")
        configure<PmdExtension> {
            toolVersion = "7.7.0"
            ruleSetFiles = files(rootProject.file("config/pmd/ruleset.xml"))
            ruleSets = emptyList() // use only our ruleset, not PMD's defaults
            isConsoleOutput = false
            isIgnoreFailures = false // tree is clean; a new violation now fails the build
        }
        tasks.matching { it.name == "pmdTest" }.configureEach { enabled = false }
        tasks.withType<Pmd>().configureEach {
            reports {
                xml.required.set(true)
                html.required.set(false)
            }
        }
    }

    apply(plugin = "com.github.spotbugs")
    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        toolVersion.set("4.8.6")
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW) // report even low-confidence findings
        ignoreFailures.set(false) // tree is clean; a new bug pattern now fails the build
        excludeFilter.set(rootProject.file("config/spotbugs/exclude.xml"))
    }
    // Only the main hand-written bytecode; tests use WireMock/JUnit patterns SpotBugs misreads.
    tasks.matching { it.name == "spotbugsTest" }.configureEach { enabled = false }
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        reports.create("xml") { required.set(true) }
        reports.create("html") { required.set(false) }
    }
}
