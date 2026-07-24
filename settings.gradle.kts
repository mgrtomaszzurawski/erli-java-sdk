rootProject.name = "erli-java-sdk"

// Reactor. Layer 1 (generated transport models) is proven first; the client, demo, examples,
// and jpms-consumer modules land next (see docs/ARCHITECTURE.md and the shared BACKLOG).
include("erli-rest-models")
