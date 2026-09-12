// PROTOTYPE — not compiled, not in the Gradle build. Throwaway sketch for nesto#64.
//
// VARIANT A — native upsert, no version column, no conflict detection.
// Product stance: "last write silently wins." Two devices editing the same node,
// the second save overwrites the first and nobody is told.
//
// Domain change: NONE. ADR 007 untouched.

// ---------- port (core) — unchanged from today ----------
public interface NodeRepositoryPort {
  void save(Node node);
  Optional<Node> findById(NodeId id);
  List<Node> findRoots();
  List<Node> findChildren(NodeId parentId);

  int softDeleteSubtree(NodeId rootId, Instant deletedAt);
  int restoreSubtree(NodeId rootId, Instant deletedAt);
  Optional<Node> findDeletedById(NodeId id);
}

// ---------- persistence: the one write path ----------
interface SpringDataNodeRepository extends JpaRepository<NodeEntity, String> {

  // created_at is deliberately absent from DO UPDATE: it is immutable.
  // The deleted_at guard lives HERE, in the single write path — not in every caller.
  @Modifying(clearAutomatically = true)
  @Transactional
  @Query(value = """
      INSERT INTO nodes (id, name, description, parent_id, position, created_at, updated_at)
      VALUES (:id, :name, :description, :parentId, :position, :createdAt, :updatedAt)
      ON CONFLICT (id) DO UPDATE SET
          name        = EXCLUDED.name,
          description = EXCLUDED.description,
          parent_id   = EXCLUDED.parent_id,
          position    = EXCLUDED.position,
          updated_at  = EXCLUDED.updated_at
      WHERE nodes.deleted_at IS NULL
      """, nativeQuery = true)
  int upsert(@Param("id") String id, @Param("name") String name,
             @Param("description") String description, @Param("parentId") String parentId,
             @Param("position") int position, @Param("createdAt") Instant createdAt,
             @Param("updatedAt") Instant updatedAt);
}

class NodePostgresqlRepository implements NodeRepositoryPort {
  private final SpringDataNodeRepository springData;

  @Override
  public void save(Node node) {
    springData.upsert(
        node.getId().value(), node.getName(), node.getDescription(),
        node.getParentId().map(NodeId::value).orElse(null),
        node.getPosition().value(), node.getCreatedAt(), node.getUpdatedAt());
    // rowcount discarded: 0 means "row is soft-deleted", which this variant
    // has no vocabulary to report. See Variant D.
  }
}

// ---------- what the use case looks like ----------
class RenameNodeUseCase {
  void handle(RenameNodeCommand cmd) {
    Node node = repository.findById(cmd.nodeId()).orElseThrow(NodeNotFound::new);
    repository.save(node.rename(cmd.newName(), clock.instant()));
    // No conflict is possible to detect, so none is reported.
  }
}

// COST:  one statement per save, no SELECT, no version column, no ADR amendment.
// PRICE: a lost update is invisible — to the user and to us.
