# ADR-005: Authentication via a single static Bearer API key

**Status:** Accepted
**Date:** 2026-07-24

## Context

Unlike marketplaces that use OAuth2 (e.g. Allegro), the Erli Marketplace API authenticates with a
single static API key issued in the shop panel (*Metoda integracji > Wlasna integracja po API*),
sent as `Authorization: Bearer <key>`. The spec's only security scheme is `bearer` (`type: http`),
applied globally.

## Decision

Model credentials as a single sealed `ApiKey` value read from the environment; send it as a Bearer
header on every request. **No OAuth2 machinery** — no grant hierarchy, no refresh-token rotation, no
single-flight refresh, no device flow, no token store. The base URL is configurable (production
default `https://erli.pl/svc/shop-api`; a BOK-allocated test environment uses a separate domain and
credentials). The key is redacted in all logs and never written to a tracked file.

## Consequences

- The auth layer is dramatically simpler than an OAuth2 SDK; most of the seed's credential guidance
  (rotation, flock token store, device polling) does not apply here.
- The single key is a secret managed via environment variables only.
- Live/test verification depends on obtaining test credentials from Erli BOK.
