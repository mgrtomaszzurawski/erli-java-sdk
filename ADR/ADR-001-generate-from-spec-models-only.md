# ADR-001: Generate Layer 1 from the OpenAPI spec (models only)

**Status:** Accepted
**Date:** 2026-07-24

## Context

Erli publishes a machine-readable OpenAPI 3.0.0 spec at
`https://erli.pl/svc/shop-api/doc/swagger.json` (54 paths, 77 schemas), rendered by Swagger UI and
kept in lock-step with the live API. We must decide whether to hand-write transport POJOs or
generate them.

## Decision

Generate Layer 1 (`*Raw` transport POJOs) from the vendored spec with `openapi-generator` (`java`
generator, `native`/Jackson library), producing **models + supporting runtime only, no api-client
classes** — the SDK reimplements transport itself. The vendored `openapi/swagger.json` is
source-of-truth, re-fetched (never hand-edited), and never appears in a diff except as a deliberate
whole-file refresh (`openapi/fetch-spec.sh`).

The upstream spec has minor non-fatal validation defects, and a few schemas trip known generator
bugs. Rather than edit the spec, a build-only `normalizeSpec` step writes a corrected copy to
`build/spec/`: it collapses `oneOf`/`anyOf` branches that contain an inline array to free-form
objects, and drops `enum` from array `items` (the generator's dead-code query-string helper
mis-types `List<Enum>` as `List<String>`). Generator spec validation is skipped.

## Consequences

- Layer 1 tracks the spec automatically; a spec refresh is a single reviewed commit.
- Generator quirks are handled in one declarative build step, not by editing generated code.
- Some polymorphic value fields are `Object`/`List<String>` at Layer 1; the domain layer re-types
  them. Revisit the workarounds if a newer generator fixes the underlying bugs.
