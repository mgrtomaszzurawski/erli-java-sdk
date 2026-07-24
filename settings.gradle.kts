rootProject.name = "erli-java-sdk"

// Reactor. Layer 1 (generated transport models) is proven first; the client, demo, examples,
// and jpms-consumer modules land next (see ADR/ADR-002 and the reactor plan).
include("erli-rest-models")
