# ADR-004: License under AGPL-3.0-only with commercial dual-licensing

**Status:** Accepted
**Date:** 2026-07-24

## Context

The SDK is a substantial engineering asset. We want open-source availability without allowing
closed-source commercial use to free-ride, and we want to retain the option to sell a commercial
license.

## Decision

License the project **AGPL-3.0-only** (the full text in `LICENSE`), offered under a dual-licensing
model: AGPL for open-source use, a separate commercial license for closed-source/commercial use.
The AGPL acts as a poison pill against unlicensed commercial embedding.

Because of dual-licensing, **external contributions are not accepted without prior owner sign-off
and a signed copyright assignment** (`CONTRIBUTING.md`). The repository is public with `develop` as
the default branch; `main` carries only tagged Maven Central releases.

## Consequences

- Commercial users must either comply with AGPL (including network-use source disclosure) or buy a
  commercial license.
- Contribution flow is deliberately gated; drive-by PRs may be closed without review.
- All published module POMs declare the AGPL-3.0 license.
