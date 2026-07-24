# ADR-004: Build with Gradle

**Status:** Accepted
**Date:** 2026-07-24

## Context

The SDK is a multi-module reactor with code generation, JPMS, a battery of quality gates, and Maven
Central publishing. Both Gradle and Maven are viable; a choice fixes the toolchain for every module.

## Decision

Use **Gradle** (Kotlin DSL, wrapper committed in-repo), a version catalog
(`gradle/libs.versions.toml`) for dependency and plugin versions, and per-module build scripts. This
matches the sibling `allegro-java-sdk` and its quality-gate roster (Spotless, Checkstyle, PMD,
SpotBugs, JaCoCo, Sonar; OWASP + PIT at release) and the vanniktech publishing plugin.

## Consequences

- Incremental builds and the generation pipeline are fast on the POD-local volume.
- Quality-gate and publish plugins are added as each is documented (an undocumented gate does not
  get run).
- Contributors need only the wrapper; no local Gradle install.
