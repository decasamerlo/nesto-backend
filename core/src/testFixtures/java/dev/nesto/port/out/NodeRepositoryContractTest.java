package dev.nesto.port.out;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.NodeMother;
import dev.nesto.domain.Position;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public abstract class NodeRepositoryContractTest {

  protected static final NodeId GRANDCHILD_ID = NodeId.of("grandchild");
  protected static final NodeId OTHER_NODE_ID = NodeId.of("other-node");
  protected static final NodeId OTHER_CHILD_ID = NodeId.of("other-child");
  protected static final NodeId ABSENT_ID = NodeId.of("absent");

  // Later than every NodeMother instant by construction, so no fixture value can equal an operation
  // instant
  protected static final Instant NOW = NodeMother.DELETED_AT.plus(1, ChronoUnit.DAYS);
  protected static final Instant LATER = NOW.plus(1, ChronoUnit.DAYS);

  protected NodeRepositoryPort repository;

  protected abstract NodeRepositoryPort createRepository();

  @BeforeEach
  void setUp() {
    repository = createRepository();
  }

  @Nested
  @DisplayName("save")
  class Save {

    // Postgres enforces this with the parent_id foreign key. Spring surfaces that as
    // DataIntegrityViolationException, which this suite cannot name, so the adapter translates it
    // to IllegalStateException
    @Test
    @DisplayName("should reject node whose parent does not exist")
    void should_reject_node_whose_parent_does_not_exist() {
      assertThatIllegalStateException()
          .isThrownBy(() -> repository.save(NodeMother.aChildNode().build()));
    }

    @Test
    @DisplayName("should reject node carrying deletedAt")
    void should_reject_node_carrying_deleted_at() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> repository.save(NodeMother.aNode().deleted().build()));
    }

    @Test
    @DisplayName("should reject save onto deleted node")
    void should_reject_save_onto_deleted_node() {
      var deletedNode = NodeMother.aNode().build();
      repository.save(deletedNode);
      repository.softDeleteSubtree(deletedNode.getId(), LATER);

      assertThatIllegalStateException()
          .isThrownBy(() -> repository.save(NodeMother.aNode().build()));
    }

    @Test
    @DisplayName("should accept child under deleted parent")
    void should_accept_child_under_deleted_parent() {
      var deletedNode = NodeMother.aNode().build();
      repository.save(deletedNode);
      repository.softDeleteSubtree(deletedNode.getId(), LATER);

      assertThatNoException().isThrownBy(() -> repository.save(NodeMother.aChildNode().build()));
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should persist node and resolve by id")
    void should_persist_node_and_resolve_by_id() {
      var node = NodeMother.aNode().build();

      repository.save(node);

      assertThat(repository.findById(node.getId())).contains(node);
    }

    @Test
    @DisplayName("should return empty when node does not exist")
    void should_return_empty_when_node_does_not_exist() {
      assertThat(repository.findById(ABSENT_ID)).isEmpty();
    }

    @Test
    @DisplayName("should return empty for deleted node")
    void should_return_empty_for_deleted_node() {
      var deletedNode = NodeMother.aNode().build();
      repository.save(deletedNode);
      repository.softDeleteSubtree(deletedNode.getId(), LATER);

      assertThat(repository.findById(deletedNode.getId())).isEmpty();
    }

    @Test
    @DisplayName("should return empty for deleted ancestor")
    void should_return_empty_for_deleted_ancestor() {
      saveTree();

      repository.softDeleteSubtree(NodeMother.NODE_ID, LATER);

      assertThat(repository.findById(GRANDCHILD_ID)).isEmpty();
    }

    @Test
    @DisplayName("should replace previous version on second save of same id")
    void should_replace_previous_version_on_second_save_of_same_id() {
      repository.save(NodeMother.aNode().build());
      var updated = NodeMother.aNode().name("new name").build();

      repository.save(updated);

      assertThat(repository.findById(updated.getId()).map(Node::getName)).contains("new name");
    }
  }

  @Nested
  @DisplayName("findRoots")
  class FindRoots {

    @Test
    @DisplayName("should return only root nodes ordered by position ascending")
    void should_return_only_root_nodes_ordered_by_position_ascending() {
      saveTree();

      assertThat(repository.findRoots())
          .extracting(Node::getId)
          .containsExactly(OTHER_NODE_ID, NodeMother.NODE_ID);
    }

    @Test
    @DisplayName("should return empty list when has no roots")
    void should_return_empty_list_when_has_no_roots() {
      assertThat(repository.findRoots()).isEmpty();
    }

    @Test
    @DisplayName("should exclude deleted roots")
    void should_exclude_deleted_roots() {
      var deletedNode = NodeMother.aNode().build();
      repository.save(deletedNode);
      repository.softDeleteSubtree(deletedNode.getId(), LATER);
      var otherNode = NodeMother.aNode().id(OTHER_NODE_ID).build();
      repository.save(otherNode);

      assertThat(repository.findRoots()).extracting(Node::getId).containsExactly(otherNode.getId());
    }

    @Test
    @DisplayName("should return defensive copy of list")
    void should_return_defensive_copy_of_list() {
      var root = NodeMother.aNode().build();
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
      saveTree();

      assertThat(repository.findChildren(NodeMother.NODE_ID))
          .extracting(Node::getId)
          .containsExactly(OTHER_CHILD_ID, NodeMother.CHILD_ID);
    }

    @Test
    @DisplayName("should return empty list when parent has no children")
    void should_return_empty_list_when_parent_has_no_children() {
      var parent = NodeMother.aNode().build();
      repository.save(parent);

      assertThat(repository.findChildren(parent.getId())).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for unknown parent")
    void should_return_empty_list_for_unknown_parent() {
      assertThat(repository.findChildren(ABSENT_ID)).isEmpty();
    }

    @Test
    @DisplayName("should exclude deleted children")
    void should_exclude_deleted_children() {
      var parent = NodeMother.aNode().build();
      repository.save(parent);
      var deletedChild = NodeMother.aChildNode().build();
      repository.save(deletedChild);
      repository.softDeleteSubtree(deletedChild.getId(), LATER);
      var otherChild = NodeMother.aChildNode().id(OTHER_CHILD_ID).build();
      repository.save(otherChild);

      assertThat(repository.findChildren(parent.getId()))
          .extracting(Node::getId)
          .containsExactly(otherChild.getId());
    }

    @Test
    @DisplayName("should return defensive copy of list")
    void should_return_defensive_copy_of_list() {
      var parent = NodeMother.aNode().build();
      var child = NodeMother.aChildNode().build();
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

      assertThat(repository.softDeleteSubtree(NodeMother.NODE_ID, LATER)).isEqualTo(4);
      assertThat(repository.findDeletedById(NodeMother.NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
      assertThat(repository.findDeletedById(NodeMother.CHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
      assertThat(repository.findDeletedById(GRANDCHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
      assertThat(repository.findById(OTHER_NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).isEmpty());
    }

    @Test
    @DisplayName("should change nothing on immediate second call")
    void should_change_nothing_on_immediate_second_call() {
      saveTree();
      repository.softDeleteSubtree(NodeMother.NODE_ID, LATER);

      assertThat(repository.softDeleteSubtree(NodeMother.NODE_ID, LATER)).isEqualTo(0);
    }

    @Test
    @DisplayName("should return zero when node does not exist")
    void should_return_zero_when_node_does_not_exist() {
      saveTree();

      assertThat(repository.softDeleteSubtree(ABSENT_ID, LATER)).isEqualTo(0);
      assertThat(repository.findDeletedById(NodeMother.NODE_ID)).isEmpty();
    }

    @Test
    @DisplayName("should leave already deleted node carrying its own instant")
    void should_leave_already_deleted_node_carrying_its_own_instant() {
      saveTree();
      repository.softDeleteSubtree(NodeMother.CHILD_ID, NOW);

      assertThat(repository.softDeleteSubtree(NodeMother.NODE_ID, LATER)).isEqualTo(2);
      assertThat(repository.findDeletedById(NodeMother.CHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(NOW));
    }
  }

  @Nested
  @DisplayName("restoreSubtree")
  class RestoreSubtree {

    @Test
    @DisplayName("should clear matching instant across subtree and return count")
    void should_clear_matching_instant_across_subtree_and_return_count() {
      saveTree();
      repository.softDeleteSubtree(NodeMother.NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NodeMother.NODE_ID, LATER)).isEqualTo(4);
      assertThat(repository.findById(NodeMother.NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).isEmpty());
      assertThat(repository.findById(NodeMother.CHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).isEmpty());
      assertThat(repository.findById(GRANDCHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).isEmpty());
    }

    @Test
    @DisplayName("should change nothing on immediate second call")
    void should_change_nothing_on_immediate_second_call() {
      saveTree();
      repository.softDeleteSubtree(NodeMother.NODE_ID, LATER);
      repository.restoreSubtree(NodeMother.NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NodeMother.NODE_ID, LATER)).isEqualTo(0);
    }

    @Test
    @DisplayName("should return zero when instant does not match")
    void should_return_zero_when_instant_does_not_match() {
      saveTree();
      repository.softDeleteSubtree(NodeMother.NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NodeMother.NODE_ID, NOW)).isEqualTo(0);
      assertThat(repository.findDeletedById(NodeMother.NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
    }

    @Test
    @DisplayName("should keep node deleted that was deleted before its ancestor")
    void should_keep_node_deleted_that_was_deleted_before_its_ancestor() {
      saveTree();
      repository.softDeleteSubtree(GRANDCHILD_ID, NOW);
      repository.softDeleteSubtree(NodeMother.NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NodeMother.NODE_ID, LATER)).isEqualTo(3);
      assertThat(repository.findDeletedById(GRANDCHILD_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(NOW));
    }

    @Test
    @DisplayName("should restore subtree to exact prior state")
    void should_restore_subtree_to_exact_prior_state() {
      var root = NodeMother.aNode().status(Node.Status.DONE).build();
      var child = NodeMother.aChildNode().position(Position.of(7)).build();
      repository.save(root);
      repository.save(child);

      repository.softDeleteSubtree(root.getId(), LATER);
      repository.restoreSubtree(root.getId(), LATER);

      assertThat(repository.findById(root.getId()))
          .get()
          .usingRecursiveComparison()
          .isEqualTo(root);
      assertThat(repository.findById(child.getId()))
          .get()
          .usingRecursiveComparison()
          .isEqualTo(child);
    }

    @Test
    @DisplayName("should leave a node outside the subtree deleted at the same instant")
    void should_leave_node_outside_subtree_deleted_at_same_instant() {
      saveTree();
      repository.softDeleteSubtree(NodeMother.NODE_ID, LATER);
      repository.softDeleteSubtree(OTHER_NODE_ID, LATER);

      assertThat(repository.restoreSubtree(NodeMother.NODE_ID, LATER)).isEqualTo(4);
      assertThat(repository.findDeletedById(OTHER_NODE_ID))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
    }

    @Test
    @DisplayName("should return zero when node does not exist")
    void should_return_zero_when_node_does_not_exist() {
      var root = NodeMother.aNode().build();
      repository.save(root);
      repository.softDeleteSubtree(root.getId(), LATER);

      assertThat(repository.restoreSubtree(ABSENT_ID, LATER)).isEqualTo(0);
      assertThat(repository.findDeletedById(root.getId())).isPresent();
    }
  }

  @Nested
  @DisplayName("findDeletedById")
  class FindDeletedById {

    @Test
    @DisplayName("should resolve deleted node")
    void should_resolve_deleted_node() {
      var node = NodeMother.aNode().build();
      repository.save(node);
      repository.softDeleteSubtree(node.getId(), LATER);

      assertThat(repository.findDeletedById(node.getId()))
          .hasValueSatisfying(n -> assertThat(n.getDeletedAt()).hasValue(LATER));
    }

    @Test
    @DisplayName("should return empty for active node")
    void should_return_empty_for_active_node() {
      var node = NodeMother.aNode().build();
      repository.save(node);

      assertThat(repository.findDeletedById(node.getId())).isEmpty();
    }

    @Test
    @DisplayName("should return empty when node does not exist")
    void should_return_empty_when_node_does_not_exist() {
      assertThat(repository.findDeletedById(ABSENT_ID)).isEmpty();
    }
  }

  protected void saveTree() {
    repository.save(NodeMother.aNode().build());
    repository.save(NodeMother.aChildNode().build());
    repository.save(NodeMother.aChildNode().id(OTHER_CHILD_ID).position(Position.of(1)).build());
    repository.save(NodeMother.aNode().id(GRANDCHILD_ID).parentId(NodeMother.CHILD_ID).build());
    repository.save(NodeMother.aNode().id(OTHER_NODE_ID).position(Position.of(1)).build());
  }

  private void assertDefensiveCopy(Supplier<List<Node>> read, Node... expected) {
    read.get().clear();
    assertThat(read.get()).containsExactly(expected);
  }
}
