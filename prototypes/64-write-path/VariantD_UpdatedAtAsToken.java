// PROTOTYPE — not compiled, not in the Gradle build. Throwaway sketch for nesto#64.
//
// VARIANT D — updatedAt IS the version. No new field, no @Version, no ADR amendment,
// and real end-to-end conflict detection.
//
// The observation: ADR 007 already guarantees that updatedAt is stamped on every
// effective change and left untouched on a no-op. That is precisely the contract a
// version counter provides. Nesto already has its concurrency token; it just has a
// domain name instead of an infrastructure one.
//
// Domain change: NONE. ADR 007 untouched — in fact ADR 007 is what makes this work.

// ---------- port: one guarded write path, guards baked in ----------
public interface NodeRepositoryPort {

  /** Inserts a new node. Fails if the id already exists. */
  void create(Node node);

  /**
   * Writes {@code node}, but only if the stored row is still at
   * {@code expectedUpdatedAt} and is not deleted.
   * @return rows written: 1 on success, 0 when the row moved on or was deleted.
   */
  int update(Node node, Instant expectedUpdatedAt);

  Optional<Node> findById(NodeId id);
  List<Node> findRoots();
  List<Node> findChildren(NodeId parentId);

  int softDeleteSubtree(NodeId rootId, Instant deletedAt);
  int restoreSubtree(NodeId rootId, Instant deletedAt);
  Optional<Node> findDeletedById(NodeId id);
}

// ---------- persistence ----------
interface SpringDataNodeRepository extends JpaRepository<NodeEntity, String> {

  // Native so @SQLRestriction is not applied. Both guards — the concurrency token
  // and the deleted check — live in this single statement. There is no second write
  // path that could forget them.
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = """
      UPDATE nodes SET
          name        = :name,
          description = :description,
          parent_id   = :parentId,
          position    = :position,
          updated_at  = :updatedAt
      WHERE id = :id
        AND updated_at = :expectedUpdatedAt
        AND deleted_at IS NULL
      """, nativeQuery = true)
  int updateIfUnchanged(@Param("id") String id, /* ... */
                        @Param("updatedAt") Instant updatedAt,
                        @Param("expectedUpdatedAt") Instant expectedUpdatedAt);
}

// ---------- the use case ----------
class RenameNodeUseCase {
  void handle(RenameNodeCommand cmd) {
    Node loaded = repository.findById(cmd.nodeId()).orElseThrow(NodeNotFound::new);
    Node renamed = loaded.rename(cmd.newName(), clock.instant());

    if (renamed == loaded) return;                       // ADR 007 no-op: nothing to write

    // ADR 007's immutability hands us the before-image for free: `loaded` is still
    // intact after the mutation, so the expected token needs no extra plumbing.
    if (repository.update(renamed, loaded.getUpdatedAt()) == 0) {
      throw new NodeChangedUnderYou(cmd.nodeId());
    }
  }
}

// WHY IT CATCHES WHAT VARIANT B MISSES
//
//   t0  phone  reads node (updatedAt = T0)
//   t1  laptop reads node (updatedAt = T0)
//   t2  phone  update(..., expected = T0)  -> 1 row. Row is now updatedAt = T2.
//   t3  laptop update(..., expected = T0)  -> 0 rows. CONFLICT, reported.
//
// The expected token comes from the user's own read, not from a re-read inside
// save(), so the whole read-edit-write window is covered.
//
// COST:  the port grows create/update instead of one save() — arguably more honest,
//        since replace-by-id was hiding two different operations;
//        a 0 rowcount cannot by itself distinguish "changed" from "deleted" from
//        "never existed" — disambiguate with a follow-up findById if the message
//        needs to differ.
// BUYS:  end-to-end detection with zero domain change, zero new columns, and both
//        guards structural rather than remembered.
//
// PRECISION CAVEAT: two effective changes landing on the same microsecond are
// indistinguishable. ADR 009 already accepted exactly this trade for the shared
// deletion instant ("at microsecond precision on a single-user app that is
// accepted rather than guarded with a batch-id column").
