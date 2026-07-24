# erli-java-sdk

Typed Java SDK for the **Erli.pl Marketplace REST API** (`https://erli.pl/svc/shop-api`).

> **Unofficial.** Not affiliated with, endorsed by, or supported by Erli sp. z o.o. It wraps the
> publicly documented Marketplace API (OpenAPI 3.0.0).

## Status

Pre-release, under active development on the `develop` branch. Not yet published to Maven Central.
Public API surface is not stable until the first tagged `v0.x` release.

## What it is

A premium, strongly-typed client over the Erli Marketplace API — immutable request/response records,
typed builders, remediation-oriented exceptions, lazy streaming pagination, and a configurable retry
policy. Layer 1 (`*Raw` transport POJOs) is generated from `openapi/swagger.json`; consumers use only
the `sdk.domain.*` surface.

## Requirements

- Java 17+
- An Erli Marketplace API key (shop panel: *Metoda integracji > Wlasna integracja po API*).

## Authentication

The API uses a single static **Bearer** API key, sent as `Authorization: Bearer <key>` on every
request. Provide it from the environment; never hard-code it.

```java
// Skeleton — the public surface is under construction (see docs/API-SURFACE.md).
try (ErliClient client = ErliClient.builder()
        .apiKey(ApiKey.of(System.getenv("ERLI_API_KEY")))
        .build()) {
    // client.orders(), client.products(), ...
}
```

The base URL is configurable; the production default is `https://erli.pl/svc/shop-api`. A separate
test environment (distinct domain + credentials) is allocated by Erli BOK on request.

## Documentation

- Upstream API reference (Swagger UI): <https://erli.pl/svc/shop-api/doc/reference/>
- Upstream guide: <https://erli.pl/svc/shop-api/doc/>
- Architecture: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- API surface plan: [`docs/API-SURFACE.md`](docs/API-SURFACE.md)
- Decisions: [`docs/ADR/`](docs/ADR/)

## License

[AGPL-3.0-only](LICENSE). Commercial licensing is available separately; see
[`CONTRIBUTING.md`](CONTRIBUTING.md) before opening a pull request.
