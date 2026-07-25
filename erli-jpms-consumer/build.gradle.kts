// erli-jpms-consumer — a MODULAR consumer whose only job is to fail the build if the SDK leaks.
// It has its own module-info that `requires io.github.mgrtomaszzurawski.erli;` and touches only the
// public surface. Because it compiles on the module path, any attempt to reference an internal or
// *Raw type would not resolve — so a green compile here is the JPMS encapsulation gate. NOT published.

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
