package dev.nesto.domain.service;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.Position;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

// The sibling-ordering rule of decasamerlo/nesto#15: where a created or moved node goes among its
// siblings, and which siblings shift to make room. Handed the siblings, it calls no port (ADR 008),
// so the use case reads them through findChildren or findRoots and saves what it returns
public final class PositionDomainService {

  // Siblings that already share a Position are ordered by id, not rejected
  private static final Comparator<Node> POSITION_THEN_ID =
      Comparator.comparing(Node::getPosition).thenComparing(node -> node.getId().value());

  // The moving node's new Position, and only the siblings that changed, never the moving node
  // itself: the use case saves the moving node with that Position, then the shifted siblings
  public record Move(Position position, List<Node> shifted) {}

  private PositionDomainService() {}

  public static Position append(Collection<Node> siblings) {
    Objects.requireNonNull(siblings, "siblings must not be null");

    return siblings.stream()
        .map(Node::getPosition)
        .max(Comparator.naturalOrder())
        .map(highest -> Position.of(Math.addExact(highest.value(), 1)))
        .orElse(Position.of(0));
  }

  public static Move move(
      Collection<Node> siblings, NodeId nodeId, Optional<NodeId> beforeSiblingId, Instant now) {
    Objects.requireNonNull(siblings, "siblings must not be null");
    Objects.requireNonNull(nodeId, "nodeId must not be null");
    Objects.requireNonNull(beforeSiblingId, "beforeSiblingId must not be null");
    Objects.requireNonNull(now, "now must not be null");

    // A reorder hands the moving node in among its own siblings. Leaving it out makes a reorder the
    // same case as a move from another parent, and leaves the slot it vacated as a gap
    List<Node> others =
        siblings.stream()
            .filter(node -> !node.getId().equals(nodeId))
            .sorted(POSITION_THEN_ID)
            .toList();

    // The index in others the node goes to: the one of the sibling it goes before, or the end
    int at = beforeSiblingId.map(id -> indexOf(others, id)).orElse(others.size());

    // Nothing to go before: create's append, so create is a move before none
    if (at == others.size()) {
      return new Move(append(others), List.of());
    }

    int right = others.get(at).getPosition().value();

    // Left + 1, or 0 at the top: where the node goes when right and the siblings after it shift up,
    // and a free value when it is below right. At the top the free value is right - 1 instead.
    // Repeated moves to the top then fill the gap below the first sibling, as repeated inserts
    // before one sibling fill the gap after left, and neither shifts until its gap runs out. A
    // long, since left + 1 passes Integer.MAX_VALUE when left sits there, and up is then
    // unavailable rather than an overflow
    long up = at == 0 ? Math.max(0, right - 1) : others.get(at - 1).getPosition().value() + 1L;

    if (up < right) {
      return new Move(Position.of((int) up), List.of());
    }

    // Where the node goes when left and the siblings before it shift down. Not floored at 0 like
    // the top's free value: below 0 is how shiftDown learns down is unavailable
    int down = right - 1;

    Optional<List<Node>> shiftedUp = shiftUp(others.subList(at, others.size()), up, now);
    Optional<List<Node>> shiftedDown = shiftDown(others.subList(0, at).reversed(), down, now);

    // The shorter of the shifts that fit wins, and up wins a tie
    if (shiftedDown.isPresent()
        && (shiftedUp.isEmpty() || shiftedDown.get().size() < shiftedUp.get().size())) {
      return new Move(Position.of(down), shiftedDown.get());
    }

    // Neither fits only when the siblings fill nearly every Position from 0 to Integer.MAX_VALUE
    return shiftedUp
        .map(shifted -> new Move(Position.of((int) up), shifted))
        .orElseThrow(() -> new ArithmeticException("no Position is free above or below the node"));
  }

  // Every sibling the move rule rejects is missing from others: one never handed in, a deleted one
  // that findChildren leaves out, one under another parent, and the moving node, filtered out above
  private static int indexOf(List<Node> nodes, NodeId id) {
    return IntStream.range(0, nodes.size())
        .filter(i -> nodes.get(i).getId().equals(id))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "sibling %s is not one of the other children".formatted(id.value())));
  }

  // Right, then each sibling after it, to one above the Position just used, until one already sits
  // higher. A sibling tied with the one before it never sits higher, so the walk separates the tie.
  // Empty when a sibling would go above Integer.MAX_VALUE: up is then unavailable, as down is below
  // 0. The node's own from passes it only when left sits there, and right with it, so the first
  // step already returns empty
  private static Optional<List<Node>> shiftUp(List<Node> run, long from, Instant now) {
    List<Node> shifted = new ArrayList<>();
    var previous = from;

    for (Node sibling : run) {
      if (sibling.getPosition().value() > previous) {
        break;
      }

      previous++;

      if (previous > Integer.MAX_VALUE) {
        return Optional.empty();
      }

      shifted.add(sibling.withPosition(Position.of((int) previous), now));
    }

    return Optional.of(shifted);
  }

  // Left, then each sibling before it, to one below the Position just used, until one already sits
  // lower. Empty when the node or a sibling would go below 0: down is then unavailable. Nothing to
  // overflow on the way down, so no Math.subtractExact
  private static Optional<List<Node>> shiftDown(List<Node> run, int from, Instant now) {
    if (from < 0) {
      return Optional.empty();
    }

    List<Node> shifted = new ArrayList<>();
    var previous = from;

    for (Node sibling : run) {
      if (sibling.getPosition().value() < previous) {
        break;
      }

      previous--;

      if (previous < 0) {
        return Optional.empty();
      }

      shifted.add(sibling.withPosition(Position.of(previous), now));
    }

    return Optional.of(shifted);
  }
}
