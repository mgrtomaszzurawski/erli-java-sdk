rootProject.name = "erli-java-sdk"

// Reactor.
//   erli-rest-models  — Layer 1, generated transport POJOs (internal, never exported).
//   erli-client       — the SDK: sdk.core + internal transport + sdk.domain facades (published).
//   erli-demo         — live end-to-end runner against the sandbox (not published).
//   erli-examples     — compile-only consumer snippets (documentation, not published).
//   erli-jpms-consumer — modular consumer that proves internal/*Raw packages do not leak.
// See ADR/ADR-002-three-layers-jpms-java17.md and context/FANOUT-PLAN.md ("Core M1").
include("erli-rest-models")
include("erli-client")
include("erli-demo")
include("erli-examples")
include("erli-jpms-consumer")
