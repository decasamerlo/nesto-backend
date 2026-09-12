// PROTOTYPE — not compiled, not in the Gradle build. Throwaway sketch for nesto#64.
//
// VARIANT C — version on the domain entity. This is the house pattern in our
// other repos: the version lives on the aggregate and the mapper bridges an
// off-by-one on the way to the row.
//
// Domain change: YES. ADR 007 gains a version field and every mutator carries it.

// ---------- core/domain/Node.java — the diff ----------
//
//   public class Node {
//     @EqualsAndHashCode.Include private final NodeId id;
//     private final String name;
//     ...
//     private final Instant updatedAt;
// +   private final long version;
//
//     public static Node create(..., Instant now) {
// -     return new Node(id, name, description, parentId.orElse(null), position, now, now);
// +     return new Node(id, name, description, parentId.orElse(null), position, now, now, 0L);
//     }
//
//     public Node rename(String newName, Instant now) {
//       if (Objects.equals(newName, this.name)) return this;
// -     return copyWith(newName, this.description, now);
// +     return copyWith(newName, this.description, now, this.version + 1);
//     }
//
// +   // ...and identically in changeDescription, withStatus (#17), withDueDate (#22),
// +   // move (#15), and every mutator ADR 007 says is still to come.
//   }

// ---------- core/domain/NodeSnapshot.java ----------
public record NodeSnapshot(
    NodeId id, String name, String description, NodeId parentId, Position position,
    Instant createdAt, Instant updatedAt, Instant deletedAt, long version) {}

// ---------- port: save can now fail ----------
public interface NodeRepositoryPort {
  void save(Node node);          // throws OptimisticLockException when the row moved on
  // ...
}

// ---------- persistence ----------
class NodePersistenceMapper {
  NodeEntity toEntity(Node domain) {
    NodeEntity e = new NodeEntity();
    // ...
    // The off-by-one: the aggregate already bumped its version in the mutator,
    // so the row we are about to UPDATE is still at version - 1.
    e.setVersion(domain.getVersion() == 0L ? 0L : domain.getVersion() - 1);
    return e;
  }
}

// ---------- the use case can finally report it ----------
class RenameNodeUseCase {
  void handle(RenameNodeCommand cmd) {
    Node node = repository.findById(cmd.nodeId()).orElseThrow(NodeNotFound::new);
    if (node.getVersion() != cmd.expectedVersion()) {   // the client sends what it read
      throw new NodeChangedUnderYou(cmd.nodeId(), node.getVersion());
    }
    repository.save(node.rename(cmd.newName(), clock.instant()));
  }
}

// COST:  ADR 007 amended; a field on Node that exists for no domain reason;
//        an off-by-one in the mapper that is a documented footgun in the precedent;
//        every future mutator must remember `version + 1` — a rule you can forget
//        exactly like a WHERE clause;
//        every bulk UPDATE must hand-bump the column too.
// BUYS:  real end-to-end conflict detection. t1-read vs t3-write IS caught.
//
// NOTE: the client must send back the version it read, so this leaks into the
//       REST contract (#26's "Whether the REST surface belongs to this map").
