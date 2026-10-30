# Nesto Backend

Java + Spring Boot API (Hexagonal Architecture, Node domain model).

Ecosystem context inherited from parent — see [AGENTS.md](../AGENTS.md) (meta-repo root). Do not duplicate shared conventions here — with one deliberate exception, below.

## Build commands

- Full suite: `./gradlew build`. One module: `./gradlew :core:test` (domain and ports) or `./gradlew :application:test` (application services).
- Single test class: `./gradlew :core:test --tests "dev.nesto.domain.NodeTest"`
- Cache-poisoned failure (stale class files in `build/`): `./gradlew :core:clean :core:test --rerun-tasks --no-build-cache`

## Module map

- `core` — domain and ports: entities (`Node`, `NodeId`, `Position`), driven ports in `port.out`, driving ports in `port.in`. Plain `java-library` (Lombok, JUnit 5 + AssertJ). No Spring or DB — unit-testable without context. New `Node` mutators follow ADR-007's pure-function shape — return a new instance, stamp `updatedAt`, return `this` on no-op ([ADR 007](../docs/adr/007-immutable-node-entity.md)).
- `application` — application services (`NodeApplicationService`), over `core` alone. Plain `java-library` (Lombok; tests use `core`'s fixtures). No Spring: a composition root, not built yet (decasamerlo/nesto#103), will build each service with a `@Bean` method. One service per aggregate implements a driving port per operation and takes a `Clock`; only application services call ports ([ADR 008](../docs/adr/008-application-layer.md)).
- `api` — Spring Boot REST adapter. **Not created yet.**

## Stack notes

- Java 25 toolchain, Gradle 9 — managed by mise, wrapper is `./gradlew`.

## Non-public sources

**Duplicated deliberately — the one shared convention kept here rather than inherited.** A standalone clone of this repo reaches no meta-repo document, and a pointer to an unreachable file tells an agent that a constraint exists without telling it what the constraint is, which invites a guess in exactly the situation the rule exists to prevent. Canonical text and reasoning: `docs/conventions/non-public-sources.md` and ADR 012 in the meta-repo (`decasamerlo/nesto`).

Nesto is public; the codebases its docs draw precedent from are not.

- **Verify before naming.** Any repository named in published text must be public: `gh repo view <owner>/<repo> --json isPrivate`, reject on `true`. Read the field — a lookup that merely succeeds proves nothing, because an agent runs authenticated as the author and sees a private repository as an entirely ordinary one.
- **Cite by shape, never by measurement.** Never publish identifiers, verbatim quotation, or counts of implementations, services, or references taken from a non-public codebase. State precedent as direction only — "the dominant approach", "a minority pattern".
- **Surfaces:** commit messages, files, issue bodies, issue comments, PR bodies, PR reviews.
- **Fires twice:** when drafting any of the above, and as a checklist item in every review pass — including over text a human wrote.
- **On a trip:** write the generic form and say that you did. Don't stall for a decision, and don't substitute silently.
