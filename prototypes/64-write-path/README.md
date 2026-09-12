# Prototype — Nesto's write path (nesto#64)

**Throwaway.** Sketches, not compiled, not in the Gradle build. They exist to make four
options concrete enough to choose between. Delete this branch once the decision lands.

## The question, restated

We were choosing a *locking mechanism* without having decided whether Nesto needs
**conflict detection**. That is a product question, not an adapter one:

> Your phone and your browser both have the same node open. Both save.
> Should the second save silently win, or should it say "this changed under you"?

Nesto has a web app and a mobile app planned, so this is a real scenario.

Answer it and the adapter follows — which is the direction the dependency should run.
Note ADR 007 phrased its deferral as *"until a real adapter with concurrent access
exists"*, framing a **client**-concurrency question in **adapter** terms. That inversion
is already baked into the ADR.

## The four options

| | Detects a lost update? | Domain change | Cost per save | ADR 007 |
|---|---|---|---|---|
| **A** upsert, no version | No | none | 1 statement | untouched |
| **B** `@Version` on entity only | **No** (see below) | none | SELECT + UPDATE | untouched |
| **C** version on `Node` | Yes | new field + every mutator | SELECT + UPDATE | **amended** |
| **D** `updatedAt` as the token | Yes | none | 1 statement | untouched |

**B is a mirage and should be struck.** It re-reads the version inside `save()`, so its
guard spans microseconds inside the adapter, not the user's read-edit-write window:

```
t0  phone  reads node (version 7)
t1  laptop reads node (version 7)
t2  phone  save(): SELECT v7 -> UPDATE WHERE version = 7 -> v8. OK.
t3  laptop save(): SELECT v8 -> UPDATE WHERE version = 8 -> OK. NO CONFLICT.
```

The phone's edit is lost just as silently as in A, having paid for a column, a SELECT on
every write, and a hand-written `version = version + 1` in every bulk UPDATE. It only
guards two threads racing inside one adapter — which a single-user app does not have.

So the real choice is **A (no detection)** vs **C or D (detection)**, and then C vs D.

## C vs D

Both detect. The difference is what they cost the domain.

**C** adds a `long version` to `Node`, and every mutator — the four that exist and the
several ADR 007 says are coming — must remember `version + 1`. That is the same class of
forgettable rule as a remembered WHERE clause, just moved into the domain. It also brings
the precedent's documented off-by-one in the mapper, and it leaks into the REST contract,
since the client has to send back the version it read.

**D** notices that `updatedAt` already *is* a version: ADR 007 guarantees it is stamped on
every effective change and left untouched on a no-op, which is exactly a version counter's
contract. The guard becomes `WHERE id = :id AND updated_at = :expected AND deleted_at IS
NULL` in the single write path — both conditions structural, neither rememberable.

ADR 007's immutability makes D *easier*: `loaded` survives the mutation, so the use case
already holds the before-image and the expected token needs no extra plumbing.

D's price: the port grows `create` + `update` instead of one `save`, and a 0 rowcount
cannot by itself tell "changed" from "deleted" from "never existed".

D's precision caveat: two effective changes on the same microsecond are indistinguishable.
ADR 009 already accepted exactly this trade for the shared deletion instant.

## Verdict

**A.** Recorded as ADR 011 in decasamerlo/nesto#71; resolution on decasamerlo/nesto#64.

**B was struck** for the reason above — and for a second one found while writing it: a
mapper-built entity always has a null version, so `isNew()` is always true and every save
would `persist()`. B does not compose with the hand-rolled mapper ADR 010 chose.

**A beat D** on exposure, not on principle. A use case per operation carries intent and
reloads server-side, so a client's stale copy never reaches the database: the window is the
overlap between two in-flight requests, and the damage is one field of one node reverting.
Moving to D later needs **no data migration**, because `updated_at` is written from day one
either way — A simply does not read it.

**D is the option to reach for if writes stop being intent-shaped.** That condition, plus
the knowingly-accepted live-orphan race (decasamerlo/nesto#70), are recorded in ADR 011 as
the triggers that reopen this.

An earlier draft of the failure scenario was wrong and is worth not repeating: adding a node
to a list and ticking a different node are writes to two different rows, so they cannot
clobber each other. Children are a query, not a stored collection. The real lost update
needs both clients writing the *same* node.
