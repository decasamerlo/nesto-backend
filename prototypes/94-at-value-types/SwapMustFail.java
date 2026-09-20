// PROTOTYPE — this file MUST NOT COMPILE. That failure is the whole result.
import java.time.Instant;

final class SwapMustFail {
  public static void main(String[] args) {
    var created = After.CreatedAt.of(Instant.parse("2026-01-01T00:00:00Z"));
    var updated = After.UpdatedAt.of(Instant.parse("2026-02-01T00:00:00Z"));

    // The exact swap that Before.java compiled without complaint:
    var wrong = new After.NodeSnapshot("n1", "Node", updated, created, null, null);
    System.out.println(wrong);
  }
}
