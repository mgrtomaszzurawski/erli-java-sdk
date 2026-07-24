#!/usr/bin/env bash
# Re-fetch the Erli Marketplace OpenAPI spec + changelog verbatim from upstream.
# The spec is source-of-truth and is NEVER hand-edited; run this, then diff and commit deliberately.
set -euo pipefail

SPEC_URL="https://erli.pl/svc/shop-api/doc/swagger.json"
CHANGELOG_URL="https://erli.pl/svc/shop-api/doc/CHANGELOG.txt"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

curl -fsS "$SPEC_URL" -o "$HERE/swagger.json"
curl -fsS "$CHANGELOG_URL" -o "$HERE/CHANGELOG.txt"

echo "Fetched:"
echo "  $HERE/swagger.json ($(wc -c < "$HERE/swagger.json") bytes)"
echo "  $HERE/CHANGELOG.txt ($(wc -c < "$HERE/CHANGELOG.txt") bytes)"
echo "Review 'git diff -- openapi/' and commit as a deliberate spec refresh."
