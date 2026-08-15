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
  private static final Instant NOW = Instant.parse("2026-05-04T10:00:00Z");

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
      var child = Node.create(NodeId.of("child-node"), "child node", DESCRIPTION, Optional.of(parent.getId()), POSITION,
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
          .isThrownBy(() -> Node.create(NODE_ID, null, DESCRIPTION, Optional.empty(), POSITION, NOW));
    }

    @Test
    @DisplayName("should reject blank name")
    void should_reject_blank_name() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Node.create(NODE_ID, "   ", DESCRIPTION, Optional.empty(), POSITION, NOW));
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
          .isThrownBy(() -> Node.create(NODE_ID, NAME, DESCRIPTION, Optional.of(NODE_ID), POSITION, NOW));
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
          .isThrownBy(() -> Node.create(NODE_ID, NAME, DESCRIPTION, Optional.empty(), POSITION, null));
    }
  }

  @Nested
  @DisplayName("Node.createdAt")
  class CreatedAt {

    @Test
    @DisplayName("should stay fixed after mutations")
    void should_stay_fixed_after_mutations() {
      var node = createNode();

      node.rename("new name", NOW.plusSeconds(5));
      node.changeDescription("new description", NOW.plusSeconds(10));

      assertThat(node.getCreatedAt()).isEqualTo(NOW);
    }
  }

  @Nested
  @DisplayName("Node.rename")
  class Rename {

    @Test
    @DisplayName("should update name and stamp updatedAt")
    void should_update_name_and_stamp_updated_at() {
      var node = createNode();
      var newName = "new name";
      var later = NOW.plusSeconds(5);

      node.rename(newName, later);

      assertThat(node.getName()).isEqualTo(newName);
      assertThat(node.getUpdatedAt()).isEqualTo(later);
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
    @DisplayName("should be no-op when name is unchanged")
    void should_be_no_op_when_name_is_unchanged() {
      var node = createNode();
      var later = NOW.plusSeconds(5);

      node.rename(NAME, later);

      assertThat(node.getName()).isEqualTo(NAME);
      assertThat(node.getUpdatedAt()).isEqualTo(NOW);
    }
  }

  @Nested
  @DisplayName("Node.changeDescription")
  class ChangeDescription {

    @Test
    @DisplayName("should update description and stamp updatedAt")
    void should_update_description_and_stamp_updated_at() {
      var node = createNode();
      var newDescription = "new description";
      var later = NOW.plusSeconds(5);

      node.changeDescription(newDescription, later);

      assertThat(node.getDescription()).isEqualTo(newDescription);
      assertThat(node.getUpdatedAt()).isEqualTo(later);
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
      var node = createNode();
      var later = NOW.plusSeconds(5);

      node.changeDescription(null, later);

      assertThat(node.getDescription()).isNull();
      assertThat(node.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("should be no-op when description is unchanged")
    void should_be_no_op_when_description_is_unchanged() {
      var node = createNode();
      var later = NOW.plusSeconds(5);

      node.changeDescription(DESCRIPTION, later);

      assertThat(node.getDescription()).isEqualTo(DESCRIPTION);
      assertThat(node.getUpdatedAt()).isEqualTo(NOW);
    }
  }

  private static Node createNode() {
    return Node.create(NODE_ID, NAME, DESCRIPTION, Optional.empty(), POSITION, NOW);
  }
}
