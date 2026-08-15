# Nesto Backend

Java + Spring Boot API server for Nesto, a personal list-of-lists app.

## What This Does

Spring Boot REST API built with Hexagonal Architecture (ports & adapters) around a self-referential `Node` domain model. All domain logic is unit-testable with zero Spring context or DB.

## Status

- **core** — domain implemented: `Node`, `NodeId`, `Position` with JUnit 5 + AssertJ tests.
- **api** — Spring Boot REST adapter. Pending.

## Tech Stack

- **Language**: Java 25
- **Framework**: Spring Boot
- **Architecture**: Hexagonal Architecture (ports & adapters)
- **Database**: Postgres
- **Migration**: Flyway
- **Build**: Gradle 9 (mise-managed, wrapper is `./gradlew`)

## Getting Started

Prerequisites: [mise](https://mise.jdx.dev) (pins Java 25 + Gradle 9) or a matching local JDK.

See [AGENTS.md](AGENTS.md#build-commands) for build commands.

## Project Layout

Module map for `core/` (domain) and `api/` (REST adapter) lives in [AGENTS.md](AGENTS.md).

## License

Personal project
