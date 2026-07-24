# ADR-005: License under AGPL-3.0-only

**Status:** Accepted
**Date:** 2026-07-24

## Context

The SDK is a substantial engineering asset published in a public repository. We need a license that
keeps it open-source while requiring anyone who builds on it — including over a network — to keep
their derivative work open under the same terms.

## Decision

License the project **AGPL-3.0-only** (full text in `LICENSE.txt`). The strong copyleft, including
the network-use (Section 13) source-disclosure requirement, is chosen deliberately. The repository
is public with `develop` as the default branch; `main` carries only tagged Maven Central releases.

## Consequences

- Downstream users must comply with AGPL, including disclosing source for network-deployed
  derivatives.
- All published module POMs declare the AGPL-3.0 license.
- Any future change to the licensing approach is a new ADR that supersedes this one.
