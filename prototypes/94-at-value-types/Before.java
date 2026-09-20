// PROTOTYPE — throwaway. Today's shape: four bare Instants in one record.
import java.time.Instant;

final class Before {
  record NodeSnapshot(
      String id, String name,
      Instant createdAt, Instant updatedAt, Instant deletedAt, Instant completedAt) {}

  public static void main(String[] args) {
    Instant created   = Instant.parse("2026-01-01T00:00:00Z");
    Instant updated   = Instant.parse("2026-02-01T00:00:00Z");
    Instant deleted   = Instant.parse("2026-03-01T00:00:00Z");
    Instant completed = Instant.parse("2026-04-01T00:00:00Z");

    // THE HAZARD: createdAt and updatedAt swapped. Nothing complains.
    var wrong = new NodeSnapshot("n1", "Node", updated, created, deleted, completed);

    System.out.println("compiled, ran, shipped: " + wrong);
    System.out.println("createdAt is really the update time: " + wrong.createdAt());
  }
}
