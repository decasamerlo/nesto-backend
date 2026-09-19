package dev.nesto.port.out;

import static org.assertj.core.api.Assertions.assertThat;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.Position;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public abstract class NodeRepositoryContractTest {

  protected static final NodeId NODE_ID = NodeId.of("node");
  protected static final NodeId CHILD_ID = NodeId.of("child");
  protected static final NodeId GRANDCHILD_ID = NodeId.of("grandchild");
  protected static final NodeId OTHER_NODE_ID = NodeId.of("other-node");
  protected static final Position POSITION = Position.of(0);
  protected static final Instant NOW = Instant.parse("2026-05-04T10:00:00Z");
  protected static final Instant LATER = Instant.parse("2026-05-04T11:00:00Z");
  protected static final Instant EARLIER = Instant.parse("2026-05-04T09:00:00Z");

  protected NodeRepositoryPort repository;

  protected abstract NodeRepositoryPort createRepository();

  @BeforeEach
  void setUp() {
    repository = createRepository();
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should persist node and resolve by id")
    void should_persist_node_and_resolve_by_id() {
      var node = createRootNode(NODE_ID);

      repository.save(node);

      assertThat(repository.findById(NODE_ID)).contains(node);
    }

    @Test
    @DisplayName("should return empty when node does not exist")
    void should_return_empty_when_node_does_not_exist() {
      assertThat(repository.findById(NODE_ID)).isEmpty();
    }

    @Test
    @DisplayName("should return empty for deleted node")
    void should_return_empty_for_deleted_node() {
      repository.save(createRootNode(NODE_ID));
      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.findById(NODE_ID)).isEmpty();
    }

    @Test
    @DisplayName("should return empty for deleted ancestor")
    void should_return_empty_for_deleted_ancestor() {
      saveTree();

      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.findById(GRANDCHILD_ID)).isEmpty();
    }

    @Test
    @DisplayName("should replace previous version on second save of same id")
    void should_replace_previous_version_on_second_save_of_same_id() {
      repository.save(createRootNode(NODE_ID));
      var updated = Node.create(NODE_ID, "new name", null, Optional.empty(), POSITION, NOW);

      repository.save(updated);

      Optional<Node> optionalNode = repository.findById(NODE_ID);
      assertThat(optionalNode.map(Node::getName)).contains("new name");
    }
  }

  @Nested
  @DisplayName("findRoots")
  class FindRoots {

    @Test
    @DisplayName("should return only root nodes ordered by position ascending")
    void should_return_only_root_nodes_ordered_by_position_ascending() {
      var root = Node.create(NODE_ID, "root", null, Optional.empty(), Position.of(3), NOW);
      var otherRoot =
          Node.create(
              NodeId.of("other-root"), "other root", null, Optional.empty(), Position.of(1), NOW);
      var child = createChildNode(CHILD_ID, root.getId(), POSITION);
      var grandchild = createChildNode(NodeId.of("grandchild"), child.getId(), POSITION);

      repository.save(root);
      repository.save(otherRoot);
      repository.save(child);
      repository.save(grandchild);

      assertThat(repository.findRoots()).containsExactly(otherRoot, root);
    }

    @Test
    @DisplayName("should return empty list when has no roots")
    void should_return_empty_list_when_has_no_roots() {
      assertThat(repository.findRoots()).isEmpty();
    }

    @Test
    @DisplayName("should return empty list when only children exist")
    void should_return_empty_list_when_only_children_exist() {
      var childA = createChildNode(NodeId.of("child-a"), NodeId.of("absent-parent"), POSITION);
      var childB =
          createChildNode(NodeId.of("child-b"), NodeId.of("absent-parent"), Position.of(1));

      repository.save(childA);
      repository.save(childB);

      assertThat(repository.findRoots()).isEmpty();
    }

    @Test
    @DisplayName("should exclude deleted roots")
    void should_exclude_deleted_roots() {
      repository.save(createRootNode(NODE_ID));
      repository.save(createRootNode(OTHER_NODE_ID));
      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.findRoots()).extracting(Node::getId).containsExactly(OTHER_NODE_ID);
    }

    @Test
    @DisplayName("should return defensive copy of list")
    void should_return_defensive_copy_of_list() {
      var root = createRootNode(NODE_ID);
      repository.save(root);

      assertDefensiveCopy(() -> repository.findRoots(), root);
    }
  }

  @Nested
  @DisplayName("findChildren")
  class FindChildren {

    @Test
    @DisplayName("should return only direct children ordered by position ascending")
    void should_return_only_direct_children_ordered_by_position_ascending() {
      var parent = createRootNode(NODE_ID);
      var firstChild = createChildNode(NodeId.of("first-child"), parent.getId(), Position.of(2));
      var secondChild = createChildNode(NodeId.of("second-child"), parent.getId(), Position.of(1));
      var grandchild = createChildNode(NodeId.of("grandchild"), firstChild.getId(), POSITION);

      repository.save(parent);
      repository.save(firstChild);
      repository.save(secondChild);
      repository.save(grandchild);

      assertThat(repository.findChildren(parent.getId())).containsExactly(secondChild, firstChild);
    }

    @Test
    @DisplayName("should return empty list when parent has no children")
    void should_return_empty_list_when_parent_has_no_children() {
      repository.save(createRootNode(NODE_ID));

      assertThat(repository.findChildren(NODE_ID)).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for unknown parent")
    void should_return_empty_list_for_unknown_parent() {
      assertThat(repository.findChildren(NODE_ID)).isEmpty();
    }

    @Test
    @DisplayName("should exclude deleted children")
    void should_exclude_deleted_children() {
      repository.save(createRootNode(NODE_ID));
      repository.save(createChildNode(CHILD_ID, NODE_ID, POSITION));
      repository.save(createChildNode(OTHER_NODE_ID, NODE_ID, Position.of(1)));
      repository.softDeleteSubtree(CHILD_ID, LATER);

      assertThat(repository.findChildren(NODE_ID))
          .extracting(Node::getId)
          .containsExactly(OTHER_NODE_ID);
    }

    @Test
    @DisplayName("should return defensive copy of list")
    void should_return_defensive_copy_of_list() {
      var parent = createRootNode(NODE_ID);
      var child = createChildNode(CHILD_ID, parent.getId(), POSITION);
      repository.save(parent);
      repository.save(child);

      assertDefensiveCopy(() -> repository.findChildren(parent.getId()), child);
    }
  }

  @Nested
  @DisplayName("softDeleteSubtree")
  class SoftDeleteSubtree {

    @Test
    @DisplayName("should stamp exactly the subtree and return the count")
    void should_stamp_exactly_the_subtree_and_return_the_count() {
      saveTree();

      assertThat(repository.softDeleteSubtree(NODE_ID, LATER)).isEqualTo(3);
      assertThat(repository.findDeletedById(NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
      assertThat(repository.findDeletedById(CHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
      assertThat(repository.findDeletedById(GRANDCHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
      assertThat(repository.findDeletedById(OTHER_NODE_ID)).isEmpty();
    }

    @Test
    @DisplayName("should change nothing on immediate second call")
    void should_change_nothing_on_immediate_second_call() {
      saveTree();
      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.softDeleteSubtree(NODE_ID, LATER)).isEqualTo(0);
    }

    @Test
    @DisplayName("should return zero when node does not exist")
    void should_return_zero_when_node_does_not_exist() {
      repository.save(createChildNode(CHILD_ID, NODE_ID, POSITION));

      assertThat(repository.softDeleteSubtree(NODE_ID, LATER)).isEqualTo(0);
      assertThat(repository.findDeletedById(CHILD_ID)).isEmpty();
    }

    @Test
    @DisplayName("should leave already deleted node carrying its own instant")
    void should_leave_already_deleted_node_carrying_its_own_instant() {
      saveTree();
      repository.softDeleteSubtree(GRANDCHILD_ID, EARLIER);

      assertThat(repository.softDeleteSubtree(NODE_ID, LATER)).isEqualTo(2);
      assertThat(repository.findDeletedById(GRANDCHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(EARLIER));
    }
  }

  @Nested
  @DisplayName("restoreSubtree")
  class RestoreSubtree {

    @Test
    @DisplayName("should clear matching instant across subtree and return count")
    void should_clear_matching_instant_across_subtree_and_return_count() {
      saveTree();
      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NODE_ID, LATER)).isEqualTo(3);
      assertThat(repository.findById(NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).isEmpty());
      assertThat(repository.findById(CHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).isEmpty());
      assertThat(repository.findById(GRANDCHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).isEmpty());
    }

    @Test
    @DisplayName("should change nothing on immediate second call")
    void should_change_nothing_on_immediate_second_call() {
      saveTree();
      repository.softDeleteSubtree(NODE_ID, LATER);
      repository.restoreSubtree(NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NODE_ID, LATER)).isEqualTo(0);
    }

    @Test
    @DisplayName("should return zero when instant does not match")
    void should_return_zero_when_instant_does_not_match() {
      saveTree();
      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NODE_ID, EARLIER)).isEqualTo(0);
    }

    @Test
    @DisplayName("should keep node deleted that was deleted before its ancestor")
    void should_keep_node_deleted_that_was_deleted_before_its_ancestor() {
      saveTree();
      repository.softDeleteSubtree(GRANDCHILD_ID, EARLIER);
      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NODE_ID, LATER)).isEqualTo(2);
      assertThat(repository.findDeletedById(GRANDCHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(EARLIER));
    }

    @Test
    @DisplayName("should restore subtree to exact prior state")
    void should_restore_subtree_to_exact_prior_state() {
      var root = createRootNode(NODE_ID).withStatus(Optional.of(Node.Status.DONE), LATER);
      var child = createChildNode(CHILD_ID, NODE_ID, Position.of(7));
      repository.save(root);
      repository.save(child);

      repository.softDeleteSubtree(NODE_ID, LATER);
      repository.restoreSubtree(NODE_ID, LATER);

      assertThat(repository.findById(NODE_ID)).get().usingRecursiveComparison().isEqualTo(root);
      assertThat(repository.findById(CHILD_ID)).get().usingRecursiveComparison().isEqualTo(child);
    }

    @Test
    @DisplayName("should leave a node outside the subtree deleted at the same instant")
    void should_leave_node_outside_subtree_deleted_at_same_instant() {
      saveTree();
      repository.softDeleteSubtree(NODE_ID, LATER);
      repository.softDeleteSubtree(OTHER_NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NODE_ID, LATER)).isEqualTo(3);
      assertThat(repository.findDeletedById(OTHER_NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
    }

    @Test
    @DisplayName("should return zero when node does not exist")
    void should_return_zero_when_node_does_not_exist() {
      repository.save(createChildNode(CHILD_ID, NODE_ID, POSITION));
      repository.softDeleteSubtree(CHILD_ID, LATER);

      assertThat(repository.restoreSubtree(NODE_ID, LATER)).isEqualTo(0);
      assertThat(repository.findDeletedById(CHILD_ID)).isPresent();
    }
  }

  @Nested
  @DisplayName("findDeletedById")
  class FindDeletedById {

    @Test
    @DisplayName("should resolve deleted node")
    void should_resolve_deleted_node() {
      var node = createRootNode(NODE_ID);
      repository.save(node);
      repository.softDeleteSubtree(NODE_ID, LATER);

      assertThat(repository.findDeletedById(NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
    }

    @Test
    @DisplayName("should return empty for active node")
    void should_return_empty_for_active_node() {
      repository.save(createRootNode(NODE_ID));

      assertThat(repository.findDeletedById(NODE_ID)).isEmpty();
    }

    @Test
    @DisplayName("should return empty when node does not exist")
    void should_return_empty_when_node_does_not_exist() {
      assertThat(repository.findDeletedById(NODE_ID)).isEmpty();
    }
  }

  protected static Node createRootNode(NodeId nodeId) {
    return Node.create(nodeId, "root name", null, Optional.empty(), POSITION, NOW);
  }

  protected static Node createChildNode(NodeId id, NodeId parentId, Position position) {
    return Node.create(id, "child name", null, Optional.of(parentId), position, NOW);
  }

  protected void saveTree() {
    repository.save(createRootNode(NODE_ID));
    repository.save(createChildNode(CHILD_ID, NODE_ID, POSITION));
    repository.save(createChildNode(GRANDCHILD_ID, CHILD_ID, POSITION));
    repository.save(
        Node.create(OTHER_NODE_ID, "other root", null, Optional.empty(), Position.of(1), NOW));
  }

  private void assertDefensiveCopy(Supplier<List<Node>> read, Node... expected) {
    read.get().clear();
    assertThat(read.get()).containsExactly(expected);
  }
}
