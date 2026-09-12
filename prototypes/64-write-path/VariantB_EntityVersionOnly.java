// PROTOTYPE — not compiled, not in the Gradle build. Throwaway sketch for nesto#64.
//
// VARIANT B — @Version on the JPA entity only, never on Node.
// This is what Q4 originally landed on. The sketch exists to show that it
// does NOT do what it looks like it does.
//
// Domain change: NONE. ADR 007 untouched.

// ---------- persistence ----------
@Entity @Table(name = "nodes")
@SQLRestriction("deleted_at IS NULL")
class NodeEntity {
  @Id private String id;
  private String name;
  private String description;
  private String parentId;
  private int position;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant deletedAt;

  @Version @Column(nullable = false)
  private Long version;   // nullable in Java so isNew() works; NOT NULL in the DB
}

class NodePostgresqlRepository implements NodeRepositoryPort {

  @Override
  public void save(Node node) {
    // A fresh mapper-built entity always has version == null, so isNew() is always
    // true and save() would always persist() -> duplicate key on update.
    // Load-or-create is the only way to keep the version attached to the row:
    NodeEntity entity = springData.findById(node.getId().value())
        .orElseGet(NodeEntity::new);        // version stays null => INSERT
    mapper.copyInto(node, entity);          // copies every field EXCEPT version
    springData.save(entity);                // Hibernate bumps version, guards the UPDATE
  }
}

// ---------- the use case is IDENTICAL to Variant A ----------
class RenameNodeUseCase {
  void handle(RenameNodeCommand cmd) {
    Node node = repository.findById(cmd.nodeId()).orElseThrow(NodeNotFound::new);
    repository.save(node.rename(cmd.newName(), clock.instant()));
  }
}

// WHY THIS IS A MIRAGE
//
//   t0  phone  reads node (version 7)
//   t1  laptop reads node (version 7)
//   t2  phone  save(): SELECT v7 -> UPDATE ... WHERE version = 7  -> row is v8. OK.
//   t3  laptop save(): SELECT v8 -> UPDATE ... WHERE version = 8  -> OK. NO CONFLICT.
//
// The version is re-read inside save(), so the guard only spans the microseconds
// between that SELECT and the flush. The user's actual read at t1 is never covered.
// The phone's edit is still silently lost, exactly as in Variant A.
//
// COST:  a SELECT on every save, a version column, an extra field to keep in sync,
//        and a hand-written `version = version + 1` in every bulk UPDATE
//        (@Modifying bypasses Hibernate's version bump).
// BUYS:  protection against two threads racing inside one adapter, which a
//        single-user app does not have.
