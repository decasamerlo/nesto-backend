# Nesto Backend

Java + Spring Boot API (Hexagonal Architecture, Node domain model).

Ecosystem context inherited from parent — see [AGENTS.md](../AGENTS.md) (meta-repo root). Do not duplicate shared conventions here.

## Build commands

- Full suite: `./gradlew build` — or `./gradlew :core:test` for domain tests only.
- Single test class: `./gradlew :core:test --tests "dev.nesto.domain.NodeTest"`
- Cache-poisoned failure (stale class files in `build/`): `./gradlew :core:clean :core:test --rerun-tasks --no-build-cache`

## Module map

- `core` — domain: entities and ports (`Node`, `NodeId`, `Position`). Plain `java-library` (Lombok, JUnit 5 + AssertJ). No Spring or DB — unit-testable without context. New `Node` mutators follow ADR-007's pure-function shape — return a new instance, stamp `updatedAt`, return `this` on no-op ([ADR 007](../docs/adr/007-immutable-node-entity.md)).
- `api` — Spring Boot REST adapter. **Not created yet.**

## Stack notes

- Java 25 toolchain, Gradle 9 — managed by mise, wrapper is `./gradlew`.
