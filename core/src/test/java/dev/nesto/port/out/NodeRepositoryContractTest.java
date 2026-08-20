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
  protected static final Position POSITION = Position.of(0);
  protected static final Instant NOW = Instant.parse("2026-05-04T10:00:00Z");

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
    @DisplayName("should return defensive copy of list")
    void should_return_defensive_copy_of_list() {
      var parent = createRootNode(NODE_ID);
      var child = createChildNode(CHILD_ID, parent.getId(), POSITION);
      repository.save(parent);
      repository.save(child);

      assertDefensiveCopy(() -> repository.findChildren(parent.getId()), child);
    }
  }

  protected static Node createRootNode(NodeId nodeId) {
    return Node.create(nodeId, "root name", null, Optional.empty(), POSITION, NOW);
  }

  protected static Node createChildNode(NodeId id, NodeId parentId, Position position) {
    return Node.create(id, "child name", null, Optional.of(parentId), position, NOW);
  }

  private void assertDefensiveCopy(Supplier<List<Node>> read, Node... expected) {
    read.get().clear();
    assertThat(read.get()).containsExactly(expected);
  }
}
