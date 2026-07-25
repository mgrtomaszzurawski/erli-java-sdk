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

// A green compile proves encapsulation, but not that the SDK RUNS as a module: Jackson serializes
// request bodies reflectively, and reflection into a package that is neither exported nor `opens`
// fails only at runtime, only on the module path. Unit tests miss it (Gradle patches them into the
// module and runs them on the classpath), and that gap already shipped a defect — see
// JpmsRuntimeCheck. So the gate also executes, on the module path.
val jpmsRuntimeCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Runs the SDK's request-encoding paths from a real module on the module path."
    // Module name comes from module-info; the class lives in the jpmsconsumer package.
    mainModule = "io.github.mgrtomaszzurawski.erli.jpms.consumer"
    mainClass = "io.github.mgrtomaszzurawski.erli.jpmsconsumer.JpmsRuntimeCheck"
    classpath = sourceSets.main.get().runtimeClasspath
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
    // Without this the jars land on the classpath and the check silently proves nothing.
    modularity.inferModulePath = true
}

tasks.named("check") {
    dependsOn(jpmsRuntimeCheck)
}
