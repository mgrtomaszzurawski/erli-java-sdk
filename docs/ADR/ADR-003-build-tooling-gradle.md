# ADR-003: Build with Gradle

**Status:** Accepted
**Date:** 2026-07-24

## Context

The SDK is a multi-module reactor with code generation, JPMS, a battery of quality gates, and Maven
Central publishing. Both Gradle and Maven are viable; a choice fixes the toolchain for every module.

## Decision

Use **Gradle** (Kotlin DSL, wrapper committed in-repo), a version catalog
(`gradle/libs.versions.toml`) for dependency and plugin versions, and per-module build scripts. This
matches the sibling `allegro-java-sdk` and the seed's Gradle gate roster
(Spotless/Checkstyle/PMD/SpotBugs/JaCoCo/Sonar; OWASP + PIT at release) and the vanniktech
publishing plugin.

## Consequences

- Incremental builds and the generation pipeline are fast on the POD-local volume.
- Quality-gate and publish plugins land as they are documented in the binding CLAUDE.md (an
  undocumented gate does not get run).
- Contributors need the wrapper only; no local Gradle install required.
