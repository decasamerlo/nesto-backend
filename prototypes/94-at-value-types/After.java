// PROTOTYPE — throwaway. Scope A: the wrappers exist ONLY in the snapshot.
import java.time.Instant;
import java.util.Objects;

final class After {

  record CreatedAt(Instant value) {
    CreatedAt { Objects.requireNonNull(value, "createdAt must not be null"); }
    static CreatedAt of(Instant v) { return new CreatedAt(v); }
  }

  record UpdatedAt(Instant value) {
    UpdatedAt { Objects.requireNonNull(value, "updatedAt must not be null"); }
    static UpdatedAt of(Instant v) { return new UpdatedAt(v); }
  }

  // Nullable fields: absence is a null REFERENCE, never a wrapper around null.
  record DeletedAt(Instant value) {
    DeletedAt { Objects.requireNonNull(value, "deletedAt must not be null"); }
    static DeletedAt of(Instant v) { return new DeletedAt(v); }
  }

  record CompletedAt(Instant value) {
    CompletedAt { Objects.requireNonNull(value, "completedAt must not be null"); }
    static CompletedAt of(Instant v) { return new CompletedAt(v); }
  }

  record NodeSnapshot(
      String id, String name,
      CreatedAt createdAt, UpdatedAt updatedAt, DeletedAt deletedAt, CompletedAt completedAt) {}

  public static void main(String[] args) {
    var created   = CreatedAt.of(Instant.parse("2026-01-01T00:00:00Z"));
    var updated   = UpdatedAt.of(Instant.parse("2026-02-01T00:00:00Z"));
    var completed = CompletedAt.of(Instant.parse("2026-04-01T00:00:00Z"));

    // A live node: deletedAt absent. Null reference, not a wrapped null.
    var right = new NodeSnapshot("n1", "Node", created, updated, null, completed);

    System.out.println("correct order compiles: " + right);
    System.out.println("unwrap at the boundary: " + right.createdAt().value());
  }
}
