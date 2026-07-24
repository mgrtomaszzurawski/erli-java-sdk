# ADR-002: Generate Layer 1 from the OpenAPI spec

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
whole-file refresh.

Because the upstream spec has minor non-fatal defects and a few schemas trip known generator bugs, a
build-only `normalizeSpec` step writes a corrected copy to `build/spec/` (collapses array-branch
`oneOf`/`anyOf` to free-form; drops array-item enums), and generator spec validation is skipped.
Details: `docs/KNOWN-SERVER-BEHAVIORS.md`.

## Consequences

- Layer 1 tracks the spec automatically; a spec refresh is a single reviewed commit.
- Generator quirks are handled in one declarative build step, not by editing generated code.
- Some polymorphic value fields are `Object` at Layer 1; the domain layer re-types them.
