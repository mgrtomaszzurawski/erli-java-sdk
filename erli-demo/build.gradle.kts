// erli-demo — live end-to-end runner against the Erli sandbox. NOT published.
// Runs on the classpath (no module-info) and reads ERLI_BASE_URL / ERLI_API_KEY from the environment
// (sourced from /workspace/shared/secrets/erli-sandbox.env — never hardcode the key). This is the
// M1 proof: `./gradlew :erli-demo:run` must return the sandbox shop from GET /me.

plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(project(":erli-client"))
}

application {
    // Defaults to the Core M1 proof; override to run a bucket's runner, e.g.
    //   ./gradlew :erli-demo:run -PmainClass=io.github.mgrtomaszzurawski.erli.demo.ErliOrdersDemo
    mainClass = providers.gradleProperty("mainClass")
        .orElse("io.github.mgrtomaszzurawski.erli.demo.ErliMeDemo")
}

// Per-bucket live proofs get their own task so `run` stays the Core M1 slice. Append yours below.
val runComms by tasks.registering(JavaExec::class) {
    group = "application"
    description = "Live proof of bucket F Comms & Automation: GET /hooks + GET /inbox + POST /inbox/_search."
    mainClass = "io.github.mgrtomaszzurawski.erli.demo.ErliCommsDemo"
    classpath = sourceSets["main"].runtimeClasspath
}
