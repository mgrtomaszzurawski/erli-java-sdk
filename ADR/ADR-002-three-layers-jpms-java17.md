# ADR-002: Three-layer architecture with JPMS (Java 17)

**Status:** Accepted
**Date:** 2026-07-24

## Context

The Erli Marketplace API is a raw REST/JSON contract. Consumers want a premium typed surface, not a
thin transport wrapper, and the internal transport/generated types must not leak into their code.

## Decision

Adopt a three-layer module architecture on Java 17 with the Java Platform Module System:

- **Layer 3 (public, exported):** `sdk.domain.*` — a single `ErliClient` (AutoCloseable) exposing
  interface domain accessors; immutable request/response records and typed builders. The only
  consumer-visible surface.
- **Layer 2 (internal, not exported):** endpoint `*Impl` wrappers, `HttpRuntime`, `ApiPaths`, retry,
  pagination, credential handling, and boundary error mapping.
- **Layer 1 (generated, not exported):** `*Raw` Jackson POJOs generated from the OpenAPI spec.

Enforce the boundary with JPMS: a `erli-jpms-consumer` module compiled against the named-module
surface, a test that parses `module-info.java` for un-exported leaks, and a javadoc element-list vs.
exports diff. `@NullMarked` at every exported `package-info.java`; internals and generated code are
never annotated.

## Consequences

- Consumers import only `sdk.domain.*`; `*Raw`/`internal.*` cannot leak.
- Refactoring Layer 1/2 is non-breaking as long as the exported surface is stable.
- Slightly more module wiring up front; paid back by an enforced public contract.
