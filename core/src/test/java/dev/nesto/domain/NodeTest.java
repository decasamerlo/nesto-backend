package dev.nesto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NodeTest {

  private static final NodeId NODE_ID = NodeId.of("node");
  private static final String NAME = "default name";
  private static final String DESCRIPTION = "default description";
  private static final Position POSITION = Position.of(0);
  private static final Instant NOW = Instant.parse("2026-01-01T01:00:00Z");
  private static final Instant LATER = Instant.parse("2026-01-02T01:00:00Z");

  @Nested
  @DisplayName("Node.create")
  class Create {

    @Test
    @DisplayName("should create root node with the given fields")
    void should_create_root_node_with_the_given_fields() {
      var node = createNode();

      assertThat(node.getId()).isEqualTo(NODE_ID);
      assertThat(node.getName()).isEqualTo(NAME);
      assertThat(node.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(node.getParentId()).isEmpty();
      assertThat(node.getPosition()).isEqualTo(POSITION);
      assertThat(node.getCreatedAt()).isEqualTo(NOW);
      assertThat(node.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("should create node without a description")
    void should_create_node_without_a_description() {
      var node = Node.create(NODE_ID, NAME, null, Optional.empty(), POSITION, NOW);

      assertThat(node.getDescription()).isNull();
    }

    @Test
    @DisplayName("should create node under an existing parent")
    void should_create_node_under_an_existing_parent() {
      var parent = createNode();
      var child =
          Node.create(
              NodeId.of("child-node"),
              "child node",
              DESCRIPTION,
              Optional.of(parent.getId()),
              POSITION,
              NOW);

      assertThat(child.getParentId()).contains(parent.getId());
    }

    @Test
    @DisplayName("should reject null id")
    void should_reject_null_id() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.create(null, NAME, DESCRIPTION, Optional.empty(), POSITION, NOW));
    }

    @Test
    @DisplayName("should reject null name")
    void should_reject_null_name() {
      assertThatNullPointerException()
          .isThrownBy(
              () -> Node.create(NODE_ID, null, DESCRIPTION, Optional.empty(), POSITION, NOW));
    }

    @Test
    @DisplayName("should reject blank name")
    void should_reject_blank_name() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () -> Node.create(NODE_ID, "   ", DESCRIPTION, Optional.empty(), POSITION, NOW));
    }

    @Test
    @DisplayName("should reject null parentId")
    void should_reject_null_parent_id() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.create(NODE_ID, NAME, DESCRIPTION, null, POSITION, NOW));
    }

    @Test
    @DisplayName("should reject self-referential parent")
    void should_reject_self_referential_parent() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () -> Node.create(NODE_ID, NAME, DESCRIPTION, Optional.of(NODE_ID), POSITION, NOW));
    }

    @Test
    @DisplayName("should reject null position")
    void should_reject_null_position() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.create(NODE_ID, NAME, DESCRIPTION, Optional.empty(), null, NOW));
    }

    @Test
    @DisplayName("should reject null now")
    void should_reject_null_now() {
      assertThatNullPointerException()
          .isThrownBy(
              () -> Node.create(NODE_ID, NAME, DESCRIPTION, Optional.empty(), POSITION, null));
    }
  }

  @Nested
  @DisplayName("Node.createdAt")
  class CreatedAt {

    @Test
    @DisplayName("should stay fixed after mutations")
    void should_stay_fixed_after_mutations() {
      var original = createNode();

      var renamed = original.rename("new name", LATER);
      var changed = original.changeDescription("new description", LATER);

      assertThat(renamed.getCreatedAt()).isEqualTo(NOW);
      assertThat(changed.getCreatedAt()).isEqualTo(NOW);

      assertThat(renamed.getParentId()).isEmpty();
      assertThat(changed.getParentId()).isEmpty();

      assertThat(renamed.getPosition()).isEqualTo(POSITION);
      assertThat(changed.getPosition()).isEqualTo(POSITION);
    }
  }

  @Nested
  @DisplayName("Node.rename")
  class Rename {

    @Test
    @DisplayName("should return new node with new name and stamped updatedAt")
    void should_return_new_node_with_new_name_and_stamped_updated_at() {
      var original = createNode();
      var newName = "new name";

      var renamed = original.rename(newName, LATER);

      assertThat(renamed.getName()).isEqualTo(newName);
      assertThat(renamed.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("should leave original node untouched")
    void should_leave_original_node_untouched() {
      var node = createNode();

      node.rename("new name", LATER);

      assertThat(node.getName()).isEqualTo(NAME);
      assertThat(node.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("should reject null name")
    void should_reject_null_name() {
      var node = createNode();

      assertThatNullPointerException().isThrownBy(() -> node.rename(null, NOW));
    }

    @Test
    @DisplayName("should reject blank name")
    void should_reject_blank_name() {
      var node = createNode();

      assertThatIllegalArgumentException().isThrownBy(() -> node.rename("   ", NOW));
    }

    @Test
    @DisplayName("should reject null now")
    void should_reject_null_now() {
      var node = createNode();

      assertThatNullPointerException().isThrownBy(() -> node.rename(NAME, null));
    }

    @Test
    @DisplayName("should return same instance when name unchanged")
    void should_return_same_instance_when_name_unchanged() {
      var original = createNode();

      var renamed = original.rename(NAME, LATER);

      assertThat(renamed).isSameAs(original);
      assertThat(renamed.getName()).isEqualTo(NAME);
      assertThat(renamed.getUpdatedAt()).isEqualTo(NOW);
    }
  }

  @Nested
  @DisplayName("Node.changeDescription")
  class ChangeDescription {

    @Test
    @DisplayName("should return new node with new description and stamped updatedAt")
    void should_return_new_node_with_new_description_and_stamped_updated_at() {
      var original = createNode();
      var newDescription = "new description";

      var changed = original.changeDescription(newDescription, LATER);

      assertThat(changed.getDescription()).isEqualTo(newDescription);
      assertThat(changed.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("should leave original node untouched")
    void should_leave_original_node_untouched() {
      var node = createNode();

      node.changeDescription("new description", LATER);

      assertThat(node.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(node.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("should reject null now")
    void should_reject_null_now() {
      var node = createNode();

      assertThatNullPointerException().isThrownBy(() -> node.changeDescription(DESCRIPTION, null));
    }

    @Test
    @DisplayName("should clear description when set to null")
    void should_clear_description_when_set_to_null() {
      var original = createNode();

      var changed = original.changeDescription(null, LATER);

      assertThat(changed.getDescription()).isNull();
      assertThat(changed.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("should return same instance when description unchanged")
    void should_return_same_instance_when_description_unchanged() {
      var original = createNode();

      var changed = original.changeDescription(DESCRIPTION, LATER);

      assertThat(changed).isSameAs(original);
      assertThat(changed.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(changed.getUpdatedAt()).isEqualTo(NOW);
    }
  }

  @Nested
  @DisplayName("Node.reconstitute")
  class Reconstitute {

    @Test
    @DisplayName("should rebuild root node with explicit timestamps")
    void should_rebuild_root_node_with_explicit_timestamps() {
      var node = Node.reconstitute(NODE_ID, NAME, DESCRIPTION, null, POSITION, NOW, LATER);

      assertThat(node.getId()).isEqualTo(NODE_ID);
      assertThat(node.getName()).isEqualTo(NAME);
      assertThat(node.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(node.getParentId()).isEmpty();
      assertThat(node.getPosition()).isEqualTo(POSITION);
      assertThat(node.getCreatedAt()).isEqualTo(NOW);
      assertThat(node.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("should rebuild child node with parent")
    void should_rebuild_child_node_with_parent() {
      var parent = createNode();
      var child =
          Node.reconstitute(
              NodeId.of("child-node"), "child node", null, parent.getId(), POSITION, NOW, LATER);

      assertThat(child.getParentId()).contains(parent.getId());
    }

    @Test
    @DisplayName("should reject null id")
    void should_reject_null_id() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.reconstitute(null, NAME, null, null, POSITION, NOW, LATER));
    }

    @Test
    @DisplayName("should reject null name")
    void should_reject_null_name() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.reconstitute(NODE_ID, null, null, null, POSITION, NOW, LATER));
    }

    @Test
    @DisplayName("should reject blank name")
    void should_reject_blank_name() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Node.reconstitute(NODE_ID, "   ", null, null, POSITION, NOW, LATER));
    }

    @Test
    @DisplayName("should reject self-referential parent")
    void should_reject_self_referential_parent() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Node.reconstitute(NODE_ID, NAME, null, NODE_ID, POSITION, NOW, LATER));
    }

    @Test
    @DisplayName("should reject null position")
    void should_reject_null_position() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.reconstitute(NODE_ID, NAME, null, null, null, NOW, LATER));
    }

    @Test
    @DisplayName("should reject null createdAt")
    void should_reject_null_created_at() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.reconstitute(NODE_ID, NAME, null, null, POSITION, null, LATER));
    }

    @Test
    @DisplayName("should reject null updatedAt")
    void should_reject_null_updated_at() {
      assertThatNullPointerException()
          .isThrownBy(() -> Node.reconstitute(NODE_ID, NAME, null, null, POSITION, NOW, null));
    }
  }

  @Nested
  @DisplayName("Node equality")
  class Equality {

    @Test
    @DisplayName("should be equal to another node with same id")
    void should_be_equal_to_another_node_with_same_id() {
      var node = createNode();
      var otherNode =
          Node.create(NODE_ID, "other name", null, Optional.empty(), Position.of(7), LATER);

      assertThat(node).isEqualTo(otherNode);
    }

    @Test
    @DisplayName("should have same hashCode as another node with same id")
    void should_have_same_hash_code_as_another_node_with_same_id() {
      var node = createNode();
      var otherNode =
          Node.create(NODE_ID, "other name", null, Optional.empty(), Position.of(7), LATER);

      assertThat(node).hasSameHashCodeAs(otherNode);
    }

    @Test
    @DisplayName("should not be equal to node with different id")
    void should_not_be_equal_to_node_with_different_id() {
      var node = createNode();
      var otherNode =
          Node.create(NodeId.of("other-node"), NAME, DESCRIPTION, Optional.empty(), POSITION, NOW);

      assertThat(node).isNotEqualTo(otherNode);
    }
  }

  private static Node createNode() {
    return Node.create(NODE_ID, NAME, DESCRIPTION, Optional.empty(), POSITION, NOW);
  }
}
