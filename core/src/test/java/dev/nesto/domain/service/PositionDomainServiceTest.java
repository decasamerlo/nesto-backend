package dev.nesto.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.NodeMother;
import dev.nesto.domain.Position;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PositionDomainServiceTest {

  // Later than every NodeMother instant by construction, so a shifted sibling's updatedAt can only
  // equal it by being stamped
  private static final Instant NOW = NodeMother.DELETED_AT.plus(1, ChronoUnit.DAYS);

  // The order the move rule gives siblings, written again so the tests do not borrow the service's
  private static final Comparator<Node> POSITION_THEN_ID =
      Comparator.comparing(Node::getPosition).thenComparing(node -> node.getId().value());

  @Nested
  @DisplayName("PositionDomainService.append")
  class Append {

    @Test
    @DisplayName("should give first child of parent with no children position 0")
    void should_give_first_child_of_parent_with_no_children_position_0() {
      assertThat(PositionDomainService.append(List.of())).isEqualTo(Position.of(0));
    }

    // The highest is neither first nor last, and 10 is not the count, so "the last one + 1" and
    // "the size" both fail it
    @Test
    @DisplayName("should give one past highest sibling whatever gaps and their order")
    void should_give_one_past_highest_sibling_whatever_gaps_and_their_order() {
      assertThat(PositionDomainService.append(siblings("B9 A0 C4"))).isEqualTo(Position.of(10));
    }

    // A plain + 1 wraps to a negative number, and fails as Position's IllegalArgumentException
    @Test
    @DisplayName("should fail as overflow past highest int not as negative position")
    void should_fail_as_overflow_past_highest_int_not_as_negative_position() {
      assertThatExceptionOfType(ArithmeticException.class)
          .isThrownBy(() -> PositionDomainService.append(List.of(sibling("A", Integer.MAX_VALUE))));
    }

    @Test
    @DisplayName("should reject null siblings")
    void should_reject_null_siblings() {
      assertThatNullPointerException()
          .isThrownBy(() -> PositionDomainService.append(null))
          .withMessage("siblings must not be null");
    }
  }

  @Nested
  @DisplayName("PositionDomainService.move")
  class Move {

    // The move rule's worked examples, one row each. A row reads: the siblings, the moving node,
    // the sibling it goes before, the moving node's new Position, the siblings shifted with their
    // new Positions, and the order that results. X is a node moving in from another parent
    @ParameterizedTest(name = "{0}: {1} before {2}")
    @DisplayName("should follow the worked examples")
    @CsvSource(
        delimiter = '|',
        textBlock =
            """
            A0 B1 C2 D3    | B | A    | 0 | A1    | B,A,C,D
            A0 B1 C2 D3    | B | D    | 3 | D4    | A,C,B,D
            A0 B1 C2 D3    | B | none | 4 |       | A,C,D,B
            A0 B1 C2 D3    | D | B    | 1 | B2 C3 | A,D,B,C
            A0 B1 C4 D5    | D | C    | 2 |       | A,B,D,C
            A0 B2 C3 D4 E5 | X | C    | 2 | B1    | A,B,X,C,D,E
            A0 B1 C1 D2    | X | C    | 2 | C3 D4 | A,B,X,C,D
                           | X | none | 0 |       | X
            """)
    void should_follow_worked_examples(
        String siblings, String moving, String before, int position, String shifted, String order) {
      var given = siblings(siblings);

      var move = PositionDomainService.move(given, NodeId.of(moving), beforeSibling(before), NOW);

      assertThat(move.position()).isEqualTo(Position.of(position));
      assertThat(written(move.shifted())).isEqualTo(shifted == null ? "" : shifted);
      // allSatisfy, since extracting(...).containsOnly(NOW) fails on a row that shifts nothing
      assertThat(move.shifted()).allSatisfy(node -> assertThat(node.getUpdatedAt()).isEqualTo(NOW));
      assertThat(orderAfter(given, moving, move)).isEqualTo(order);
    }

    // The move rule's step 4 at the top, which none of its worked examples covers. Taking 0 also
    // orders X first, but the next move to the top would then find no free value and shift
    @Test
    @DisplayName("should take one below first sibling when sits above 0 shifting nothing")
    void should_take_one_below_first_sibling_when_sits_above_0_shifting_nothing() {
      var given = siblings("A3 B4");

      var move = PositionDomainService.move(given, NodeId.of("X"), beforeSibling("A"), NOW);

      assertThat(move.position()).isEqualTo(Position.of(2));
      assertThat(move.shifted()).isEmpty();
    }

    // Counting D among its own siblings gives 4
    @Test
    @DisplayName("should leave moving node out of own siblings")
    void should_leave_moving_node_out_of_own_siblings() {
      var given = siblings("A0 B1 C2 D3");

      var move = PositionDomainService.move(given, NodeId.of("D"), beforeSibling("none"), NOW);

      assertThat(move.position()).isEqualTo(Position.of(3));
      assertThat(move.shifted()).isEmpty();
    }

    // ULID-shaped ids, so the message assertion cannot pass on the message's own words. The first
    // three are never handed in, which is how each reaches the service. The moving node is, between
    // A and C, so a move that kept it among the others would find it there and go red
    @ParameterizedTest(name = "{0}")
    @DisplayName(
        "should reject sibling to go before that is not one of target parent's other children")
    @CsvSource(
        delimiter = '|',
        textBlock =
            """
            absent                                 | 01K9PQ3V7R2M8XWJ4TN6HC5BYD
            deleted, so findChildren leaves it out | 01K9PQ4A2F8KZ3VN7RX1MJ6TQE
            under another parent, so not handed in | 01K9PQ4H9W5CY2DB8LS3GV7NRF
            the moving node itself                 | 01K9PQ4R1T6JX9QE3WM5ZK8CPA
            """)
    void should_reject_sibling_to_go_before_that_is_not_one_of_target_parent_s_other_children(
        String name, String beforeSiblingId) {
      var movingNodeId = "01K9PQ4R1T6JX9QE3WM5ZK8CPA";
      var given = List.of(sibling("A", 0), sibling(movingNodeId, 1), sibling("C", 2));

      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(
              () ->
                  PositionDomainService.move(
                      given, NodeId.of(movingNodeId), beforeSibling(beforeSiblingId), NOW))
          .withMessageContaining(beforeSiblingId);
    }

    // Handed in C first, so a service that kept the order it was handed would put C before B
    @ParameterizedTest(name = "X before {0}")
    @DisplayName("should order siblings sharing position by id whatever order they come in")
    @CsvSource(
        delimiter = '|',
        textBlock =
            """
            B | 0 |    | X,B,C
            C | 2 | C3 | B,X,C
            """)
    void should_order_siblings_sharing_position_by_id_whatever_order_they_come_in(
        String before, int position, String shifted, String order) {
      var given = siblings("C1 B1");

      var move = PositionDomainService.move(given, NodeId.of("X"), beforeSibling(before), NOW);

      assertThat(move.position()).isEqualTo(Position.of(position));
      assertThat(written(move.shifted())).isEqualTo(shifted == null ? "" : shifted);
      assertThat(orderAfter(given, "X", move)).isEqualTo(order);
    }

    // B leaves 1 empty, and the gaps around C stay: nothing moves into either
    @Test
    @DisplayName("should leave gaps uncompacted including slot node vacated")
    void should_leave_gaps_uncompacted_including_slot_node_vacated() {
      var given = siblings("A0 B1 C5 D9");

      var move = PositionDomainService.move(given, NodeId.of("B"), beforeSibling("D"), NOW);

      assertThat(move.position()).isEqualTo(Position.of(6));
      assertThat(move.shifted()).isEmpty();
    }

    // Going last has no down to fall back on. A plain + 1 wraps to a negative number and fails as
    // Position's IllegalArgumentException instead
    @Test
    @DisplayName("should fail as overflow when going last past highest int")
    void should_fail_as_overflow_when_going_last_past_highest_int() {
      var given = List.of(sibling("A", Integer.MAX_VALUE));

      assertThatExceptionOfType(ArithmeticException.class)
          .isThrownBy(
              () -> PositionDomainService.move(given, NodeId.of("X"), Optional.empty(), NOW));
    }

    // Positions written as their distance below Integer.MAX_VALUE, "A0" sitting at it. Up would
    // take a node past it in each row, so down answers: in the second row too, where up would
    // shift one sibling and down shifts three
    @ParameterizedTest(name = "{0}")
    @DisplayName("should shift down when shifting up would pass highest int")
    @CsvSource(
        delimiter = '|',
        textBlock =
            """
            left at highest int            | A0 B0       | B | 1 | A2       | A,X,B
            up shifting fewer, past it     | A3 B2 C1 D0 | D | 1 | A4 B3 C2 | A,B,C,X,D
            down shifting fewer            | A4 B2 C1 D0 | C | 2 | B3       | A,B,X,C,D
            """)
    void should_shift_down_when_shifting_up_would_pass_highest_int(
        String name, String siblings, String before, int position, String shifted, String order) {
      var given = belowHighestInt(siblings);

      var move = PositionDomainService.move(given, NodeId.of("X"), beforeSibling(before), NOW);

      assertThat(move.position()).isEqualTo(Position.of(Integer.MAX_VALUE - position));
      assertThat(writtenBelowHighestInt(move.shifted())).isEqualTo(shifted);
      assertThat(orderAfter(given, "X", move)).isEqualTo(order);
    }

    // Each message is asserted, since without their checks null siblings and a null sibling to go
    // before still fail with a NullPointerException, from the stream and from the Optional
    @Test
    @DisplayName("should reject null siblings")
    void should_reject_null_siblings() {
      assertThatNullPointerException()
          .isThrownBy(() -> PositionDomainService.move(null, NodeId.of("X"), Optional.empty(), NOW))
          .withMessage("siblings must not be null");
    }

    @Test
    @DisplayName("should reject null node id")
    void should_reject_null_node_id() {
      var given = siblings("A0 B1");

      assertThatNullPointerException()
          .isThrownBy(() -> PositionDomainService.move(given, null, Optional.empty(), NOW))
          .withMessage("nodeId must not be null");
    }

    @Test
    @DisplayName("should reject null sibling to go before")
    void should_reject_null_sibling_to_go_before() {
      var given = siblings("A0 B1");

      assertThatNullPointerException()
          .isThrownBy(() -> PositionDomainService.move(given, NodeId.of("X"), null, NOW))
          .withMessage("beforeSiblingId must not be null");
    }

    // Before none shifts nothing, so a missing check cannot hide behind withPosition's own
    @Test
    @DisplayName("should reject null now even when nothing shifts")
    void should_reject_null_now_even_when_nothing_shifts() {
      var given = siblings("A0 B1");

      assertThatNullPointerException()
          .isThrownBy(
              () -> PositionDomainService.move(given, NodeId.of("X"), Optional.empty(), null))
          .withMessage("now must not be null");
    }
  }

  // Every move over a small domain: up to four siblings at Positions 0 to 5, ties included, each
  // sibling or a newcomer X moving, before each other sibling or none
  @Nested
  @DisplayName("PositionDomainService.move, over every small sibling list")
  class EverySmallMove {

    private record Case(List<Node> siblings, String moving, Optional<NodeId> before) {

      PositionDomainService.Move move() {
        return PositionDomainService.move(siblings, NodeId.of(moving), before, NOW);
      }

      List<Node> others() {
        return siblings.stream().filter(node -> !node.getId().value().equals(moving)).toList();
      }

      @Override
      public String toString() {
        return "%s: %s before %s"
            .formatted(written(siblings), moving, before.map(NodeId::value).orElse("none"));
      }
    }

    // The service's postcondition. The moving node's old Position is no longer held, so only the
    // others need to be distinct for the result to be
    @Test
    @DisplayName("should hand back distinct positions whenever handed distinct ones")
    void should_hand_back_distinct_positions_whenever_handed_distinct_ones() {
      for (var each : everySmallMove()) {
        var others = each.others();
        if (others.stream().map(Node::getPosition).distinct().count() < others.size()) {
          continue;
        }

        var move = each.move();

        assertThat(positionsAfter(others, move)).as(each.toString()).doesNotHaveDuplicates();
      }
    }

    // Ties included: a tie the move leaves alone is still ordered by id
    @Test
    @DisplayName("should place node right before sibling it names keeping others in order")
    void should_place_node_right_before_sibling_it_names_keeping_others_in_order() {
      for (var each : everySmallMove()) {
        var expected =
            new ArrayList<>(
                each.others().stream()
                    .sorted(POSITION_THEN_ID)
                    .map(node -> node.getId().value())
                    .toList());
        var at = each.before().map(id -> expected.indexOf(id.value())).orElse(expected.size());
        expected.add(at, each.moving());

        var move = each.move();

        assertThat(orderAfter(each.siblings(), each.moving(), move))
            .as(each.toString())
            .isEqualTo(String.join(",", expected));
      }
    }

    @Test
    @DisplayName("should return only siblings it changed each stamped now")
    void should_return_only_siblings_it_changed_each_stamped_now() {
      for (var each : everySmallMove()) {
        var before =
            each.others().stream()
                .collect(Collectors.toMap(node -> node.getId().value(), Node::getPosition));

        var move = each.move();

        assertThat(move.shifted())
            .as(each.toString())
            .extracting(node -> node.getId().value())
            .doesNotHaveDuplicates()
            .allSatisfy(id -> assertThat(before).containsKey(id));
        assertThat(move.shifted())
            .as(each.toString())
            .allSatisfy(
                node -> {
                  assertThat(node.getPosition()).isNotEqualTo(before.get(node.getId().value()));
                  assertThat(node.getUpdatedAt()).isEqualTo(NOW);
                });
      }
    }

    private static List<Position> positionsAfter(
        List<Node> others, PositionDomainService.Move move) {
      var shifted =
          move.shifted().stream()
              .collect(Collectors.toMap(node -> node.getId().value(), Node::getPosition));
      var positions = new ArrayList<Position>();
      for (var node : others) {
        positions.add(shifted.getOrDefault(node.getId().value(), node.getPosition()));
      }
      positions.add(move.position());
      return positions;
    }

    private static List<Case> everySmallMove() {
      var cases = new ArrayList<Case>();
      for (var size = 0; size <= 4; size++) {
        var lists = (int) Math.pow(6, size);
        for (var code = 0; code < lists; code++) {
          var siblings = new ArrayList<Node>();
          var rest = code;
          for (var i = 0; i < size; i++) {
            siblings.add(sibling(String.valueOf((char) ('A' + i)), rest % 6));
            rest /= 6;
          }
          var movers =
              new ArrayList<>(siblings.stream().map(node -> node.getId().value()).toList());
          movers.add("X");
          for (var moving : movers) {
            cases.add(new Case(siblings, moving, Optional.empty()));
            for (var before : siblings) {
              if (!before.getId().value().equals(moving)) {
                cases.add(new Case(siblings, moving, Optional.of(before.getId())));
              }
            }
          }
        }
      }
      return cases;
    }
  }

  // "none" is no sibling to go before
  private static Optional<NodeId> beforeSibling(String written) {
    return "none".equals(written) ? Optional.empty() : Optional.of(NodeId.of(written));
  }

  // The shifted siblings, written as the siblings are, by id
  private static String written(List<Node> nodes) {
    return nodes.stream()
        .map(node -> node.getId().value() + node.getPosition().value())
        .sorted()
        .collect(Collectors.joining(" "));
  }

  // The ids in Position order once the move is saved, the moving node included
  private static String orderAfter(
      List<Node> siblings, String moving, PositionDomainService.Move move) {
    var shifted =
        move.shifted().stream()
            .collect(Collectors.toMap(node -> node.getId().value(), node -> node));
    var after = new ArrayList<Node>();

    for (var node : siblings) {
      if (!node.getId().value().equals(moving)) {
        after.add(shifted.getOrDefault(node.getId().value(), node));
      }
    }
    after.add(sibling(moving, move.position().value()));

    return after.stream()
        .sorted(POSITION_THEN_ID)
        .map(node -> node.getId().value())
        .collect(Collectors.joining(","));
  }

  // Each sibling written as its id and its Position, "A0 B1", as the move rule's worked examples
  // are
  private static List<Node> siblings(String written) {
    if (written == null) {
      return List.of();
    }

    return Arrays.stream(written.split(" "))
        .filter(cell -> !cell.isEmpty())
        .map(cell -> sibling(cell.substring(0, 1), Integer.parseInt(cell.substring(1))))
        .toList();
  }

  // Each sibling written as its id and its distance below Integer.MAX_VALUE, "A0" sitting at it
  private static List<Node> belowHighestInt(String written) {
    return siblings(written).stream()
        .map(node -> sibling(node.getId().value(), Integer.MAX_VALUE - node.getPosition().value()))
        .toList();
  }

  // The shifted siblings, written as belowHighestInt reads them
  private static String writtenBelowHighestInt(List<Node> nodes) {
    return nodes.stream()
        .map(node -> node.getId().value() + (Integer.MAX_VALUE - node.getPosition().value()))
        .sorted()
        .collect(Collectors.joining(" "));
  }

  private static Node sibling(String id, int position) {
    return NodeMother.aChildNode().id(NodeId.of(id)).position(Position.of(position)).build();
  }
}
