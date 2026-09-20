// PROTOTYPE — the recommended scope, end to end.
// Wrappers are FIELD types. Getters unwrap. Mutators keep a bare Instant.
import java.time.Instant;
import java.util.Optional;

final class NodeSketch {
  private final String name;
  private final After.CreatedAt createdAt;
  private final After.UpdatedAt updatedAt;
  private final After.CompletedAt completedAt;   // nullable

  private NodeSketch(String name, After.CreatedAt createdAt,
                     After.UpdatedAt updatedAt, After.CompletedAt completedAt) {
    this.name = name;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.completedAt = completedAt;
  }

  // Callers see Instant. 36 existing call sites in core do not change.
  public Instant getCreatedAt() { return createdAt.value(); }
  public Instant getUpdatedAt() { return updatedAt.value(); }
  public Optional<Instant> getCompletedAt() {
    return Optional.ofNullable(completedAt).map(After.CompletedAt::value);
  }

  // A clock reading has no single role, so it stays a bare Instant.
  static NodeSketch create(String name, Instant now) {
    return new NodeSketch(name, After.CreatedAt.of(now), After.UpdatedAt.of(now), null);
  }

  NodeSketch rename(String newName, Instant now) {
    if (newName.equals(this.name)) return this;
    // The internal constructor call — 5 adjacent same-typed args today — is
    // what this scope protects. Swap the next two and it stops compiling.
    return new NodeSketch(newName, this.createdAt, After.UpdatedAt.of(now), this.completedAt);
  }

  public static void main(String[] args) {
    var n = create("Groceries", Instant.parse("2026-01-01T00:00:00Z"))
              .rename("Shopping", Instant.parse("2026-02-01T00:00:00Z"));
    System.out.println("createdAt " + n.getCreatedAt() + " | updatedAt " + n.getUpdatedAt()
        + " | completedAt " + n.getCompletedAt());
  }
}
