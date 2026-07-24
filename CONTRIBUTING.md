# Contributing

This project is licensed under **AGPL-3.0-only** and is offered under a dual-licensing model: the
AGPL for open-source use, and a separate commercial license for closed-source/commercial use.

Because of the dual-licensing model, **external contributions are not accepted without prior sign-off
from the project owner and a signed copyright assignment**. Please open an issue to discuss any
proposed change before writing code. Pull requests submitted without prior agreement may be closed
without review.

## Working conventions (maintainers)

- English in all code, comments, and docs.
- Never commit to `main` or `develop`. Branch `feature/<topic>` off `develop`, open a PR targeting
  `develop`, squash-merge and keep the branch.
- `openapi/swagger.json` is source-of-truth and is re-fetched, never hand-edited — it must never
  appear in a diff.
- No AI/tooling attribution anywhere in commits, PRs, code, or docs.
