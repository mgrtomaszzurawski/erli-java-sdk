# Architecture

`erli-java-sdk` is a premium typed client over the Erli.pl Marketplace REST/JSON API. It follows a
three-layer design with JPMS, generating Layer 1 from the OpenAPI spec and reimplementing transport
itself (no vendor SDK wrapped).

## Layers

- **Layer 3 — public API** (`sdk.domain.*`): the only consumer-visible surface. A single
  `ErliClient` (AutoCloseable) exposes interface domain accessors (`client.products()`,
  `client.orders()`, `client.shipping()`, …). Each `domain/<feature>/` holds a Facade + `builder/` +
  `model/` of immutable request/response records. Consumers never see Layer 1 or 2.
- **Layer 2 — internal** (not JPMS-exported): endpoint `*Impl` wrappers, a narrow `HttpRuntime`
  over `java.net.http.HttpClient`, an `ApiPaths` constant class, retry, pagination, credential
  handling, and error mapping. Maps transport to domain exceptions at the boundary.
- **Layer 1 — generated** (not exported): `*Raw` Jackson POJOs in
  `io.github.mgrtomaszzurawski.erli.rest.model`, generated at build time from
  `openapi/swagger.json`. Never hand-edited, never committed.

## Layer-1 generation (module `erli-rest-models`)

- Generator: `openapi-generator` `java` generator, `native` (Jackson) library — **models +
  supporting runtime only, no api-client classes** (`globalProperties = models + supportingFiles`,
  no `apis`).
- The vendored spec is source-of-truth and pristine. A build-only `normalizeSpec` step writes a
  corrected copy to `build/spec/` to work around generator bugs (array-branch `oneOf`/`anyOf` →
  free-form; array-item enums dropped). See [KNOWN-SERVER-BEHAVIORS.md](KNOWN-SERVER-BEHAVIORS.md).
- Validation is skipped (`skipValidateSpec`) because the upstream spec has minor non-fatal defects.
- All `JavaCompile` tasks use UTF-8 (generated Javadoc carries Polish text from the spec).

## Reactor (target)

Multi-module Gradle. Proven now: `erli-rest-models` (Layer 1). Landing next:

| Module | Layer | Role |
|---|---|---|
| `erli-rest-models` | 1 | generated `*Raw` transport POJOs (done) |
| `erli-client` | 2 + 3 | `ErliClient`, domain facades, transport runtime, JPMS surface |
| `erli-demo` | — | live/sandbox exploration runner (never published) |
| `erli-examples` | — | consumer-facing snippets (never published) |
| `erli-jpms-consumer` | — | compiles against the named-module surface to prove exports |

## Authentication

A single static Bearer API key (`ApiKey`, sealed, read from env), sent as `Authorization: Bearer
<key>`. No OAuth2, no refresh/rotation. Base URL configurable (prod
`https://erli.pl/svc/shop-api`; a BOK-allocated test environment uses a separate domain + creds).
The key is redacted in all logs.

## Cross-cutting API design

- **Exceptions grouped by remediation** on Erli codes `1100`/`1200`/`1300`/`1400`
  (server/validation/auth/not-found); `traceId`/`spanId` carried in the payload; `polishMessage`
  preserved.
- **Retry**: immutable `RetryPolicy`, equal-jitter backoff, `Retry-After` honored as a floor on 429.
- **Pagination**: lazy `Stream<T>` over the body cursor (`_search` → `pagination.after`); never a
  `listAll()`.
- **Async-by-spec operations are sync-default** with an optional timeout overload; no
  `CompletableFuture` in the public surface.
- **AutoCloseable client** with a volatile closed flag and `ensureOpen()` on every accessor.

## JPMS gates (to land with the client module)

A `erli-jpms-consumer` module compiling against the named-module surface; a test parsing
`module-info.java` for un-exported leaks; a javadoc element-list vs. exports diff. `@NullMarked` at
every exported `package-info.java`; internals and generated code are never annotated.
