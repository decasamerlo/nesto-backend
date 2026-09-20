# Prototype — typed timestamp fields (decasamerlo/nesto#94)

**Throwaway.** Four files, not in the Gradle build. Unlike the write-path
sketches on `prototype/64-write-path`, these **do compile** — the whole result
is which of them compiles and which does not. Delete this branch once #94 lands.

## The question

`Node` holds five `Instant` fields, four of them adjacent in every positional
call. Does giving them **distinct types** — `CreatedAt`, `UpdatedAt`,
`DeletedAt`, `CompletedAt` — actually stop a mis-ordered rebuild from compiling,
and what does it cost?

It came out of the survey behind #14. The hexagonal Java projects that keep a
separate persistence model overwhelmingly use a long positional factory and have
**no swap hazard at all**, because no two adjacent parameters share a type —
[thombergs/buckpal](https://github.com/thombergs/buckpal) and
[ddd-by-examples/library](https://github.com/ddd-by-examples/library).
[ADR 010](https://github.com/decasamerlo/nesto/blob/main/docs/adr/010-node-persistence-seam.md)
reached for a snapshot record instead and never weighed this option; that gap is
recorded there now, corrected in decasamerlo/nesto#96.

## Run it

```bash
javac -d out Before.java After.java NodeSketch.java
java -cp out Before
java -cp out NodeSketch
javac -d out -cp out SwapMustFail.java   # MUST fail — that failure is the result
```

## What each file is for

| File | Shows |
| --- | --- |
| `Before.java` | today's shape — a snapshot built with `createdAt`/`updatedAt` swapped, which compiles, runs, and prints a node whose creation time is its update time |
| `After.java` | the four wrappers and a typed snapshot; correct order still compiles |
| `SwapMustFail.java` | the identical swap against the typed record — **does not compile** |
| `NodeSketch.java` | the recommended scope end to end: typed *fields*, getters that unwrap to `Instant`, mutators that still take a bare `Instant now` |

The failure that matters:

```text
SwapMustFail.java:10: error: incompatible types:
    UpdatedAt cannot be converted to CreatedAt
```

## What it settled

**Three scopes, measured against `core`.** Typing only the snapshot's components
leaves `Node`'s own private constructor unguarded — four call sites, five
adjacent same-typed arguments. Typing the getters as well costs **36** further
call sites across four files and guards nothing extra, because getters are
called one at a time and adjacency is what creates the hazard.

**The middle scope wins:** wrappers as field types, getters that unwrap. Full
guarantee, churn confined almost entirely to `Node.java`. `NodeSketch.java`
demonstrates it, and swapping the two arguments in its `rename` still fails to
compile.

**The clock reading stays a bare `Instant`.** `withStatus(status, now)` turns one
`now` into *both* `updatedAt` and `completedAt`, so it maps to no single role —
the same argument
[test-fixtures.md](https://github.com/decasamerlo/nesto/blob/main/docs/conventions/test-fixtures.md)
already makes about naming. The wrappers stop at the stored fields.

**Absence gets one encoding.** Each wrapper rejects a null value, so absence is
always a null reference and never a wrapper around null — the shape `NodeId`
already has.

## Verdict

Worth doing, tracked as decasamerlo/nesto#94, blocked by
decasamerlo/nesto#14 — the adapter introduces `NodeSnapshot`, and doing these in
the other order means writing the mapper twice.
