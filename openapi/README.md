# OpenAPI spec (source-of-truth for Layer 1)

`swagger.json` is the Erli Marketplace API contract (OpenAPI 3.0.0), fetched verbatim from:

    https://erli.pl/svc/shop-api/doc/swagger.json

`CHANGELOG.txt` is the upstream change log, from:

    https://erli.pl/svc/shop-api/doc/CHANGELOG.txt

## Rules

- **Never hand-edit `swagger.json`.** It is re-fetched, not modified. It must never appear in a git
  diff except as a deliberate, whole-file refresh (`./fetch-spec.sh`, reviewed as its own commit).
- Layer 1 (`*Raw` transport POJOs) is generated from this file at build time by the
  `erli-rest-models` module; generated sources are not committed.
- First vendored: 2026-07-24 (upstream CHANGELOG top entry: 2026-06-09).

To refresh:

    ./fetch-spec.sh   # re-downloads swagger.json + CHANGELOG.txt, then diff and commit deliberately
