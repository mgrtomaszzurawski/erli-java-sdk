# erli-java-sdk

A typed Java SDK for the [Erli.pl Marketplace REST API](https://erli.pl/svc/shop-api/doc/).

> **Status: pre-release (`0.0.1-SNAPSHOT`).** Under active development on `develop`. The public API
> is not stable and it is not yet published to Maven Central.

> **Unofficial and unaffiliated.** This is an independent, community-built SDK. It is **not** an
> official Erli product and is **not affiliated with, endorsed, sponsored, or supported by Erli**.
> "Erli" is a trademark of its respective owner; the name is used here solely to identify the
> third-party REST API this library targets (nominative use). The software is provided "as is" under
> AGPL-3.0-only, without warranty of any kind. You are responsible for using it in accordance with
> Erli's API terms and conditions.

## About

This SDK provides a premium, strongly-typed surface over the Erli Marketplace API: a single
`ErliClient` entry point exposing domain accessors (products, orders, inbox, payments, shipping,
delivery, dictionaries, billing, commissions, campaigns, hooks, account), Bearer-key authentication,
resilient transport with a configurable retry policy, and lazy `Stream`-based pagination.

Data-transfer types are generated from Erli's
[OpenAPI 3.0 specification](https://erli.pl/svc/shop-api/doc/swagger.json); the client, transport,
authentication, and domain facades are hand-written.

## Architecture

Three layers, enforced by the Java Platform Module System (JPMS):

| Layer | Module | Exported | Contents |
|---|---|---|---|
| 3 — public API | `erli-client` | yes | `ErliClient`, `sdk.domain.*` facades + builders + immutable models, `config`, `core`, `exception` |
| 2 — internal | `erli-client` (`…erli.internal`) | no | `*Impl` endpoint wrappers, `HttpRuntime`, `ApiPaths`, retry, pagination |
| 1 — generated | `erli-rest-models` | no | `*Raw` OpenAPI POJOs |

Consumers depend only on `sdk.domain.*` — never on `internal.*`, `*Raw`, or transport types.
Decisions are recorded in [`ADR/`](ADR/).

## Requirements

- Java 17+
- An Erli Marketplace API key (shop panel: *Metoda integracji > Wlasna integracja po API*).

## Authentication

The API uses a single static **Bearer** API key, sent as `Authorization: Bearer <key>` on every
request. Provide it from the environment; never hard-code it. The base URL is configurable (the
production default is `https://erli.pl/svc/shop-api`; a separate test environment is allocated by
Erli on request).

```java
// Skeleton — the public surface is under construction.
try (ErliClient client = ErliClient.builder()
        .apiKey(ApiKey.of(System.getenv("ERLI_API_KEY")))
        .build()) {
    // client.orders(), client.products(), ...
}
```

## Guides

Per-feature guides land with the domain they document.

| Guide | Covers |
|---|---|
| [Finance](docs/finance.md) | payments, payouts, billing ledger, commission estimates, campaign spend |

## License

[AGPL-3.0-only](LICENSE.txt).
