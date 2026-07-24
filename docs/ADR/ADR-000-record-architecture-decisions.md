# ADR-000: Record architecture decisions

**Status:** Accepted
**Date:** 2026-07-24

## Context

The SDK is built by multiple agents over time. Decisions need a durable, append-only record so a
choice is not silently re-litigated or reversed.

## Decision

We keep Architecture Decision Records (ADRs) under `docs/ADR/`, numbered sequentially. ADRs are
**immutable**: to change direction, mark the old ADR `Superseded by [ADR-XXX]` (leave its body) and
write a new one with `Supersedes: ADR-OLD`. Small still-valid clarifications go in a dated
**Amendment** section at the end; full reversals require supersession. This immutability applies to
our own later edits, not just external ones.

## Consequences

- A decision's history is readable in place; the current state is the newest non-superseded ADR on
  a topic.
- ADR files never receive in-place semantic edits — reviewers reject those.
