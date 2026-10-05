package dev.nesto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NodeTest {

  // Later than every NodeMother instant by construction, so no fixture value can equal an operation
  // instant
  private static final Instant NOW = NodeMother.DELETED_AT.plus(1, ChronoUnit.DAYS);
  private static final Instant LATER = NOW.plus(1, ChronoUnit.DAYS);

  @Nested
  @DisplayName("Node.create")
  class Create {

    @Test
    @DisplayName("should create root node with the given fields")
    void should_create_root_node_with_the_given_fields() {
      var node =
          Node.create(
              NodeMother.NODE_ID,
              NodeMother.NAME,
              NodeMother.DESCRIPTION,
              Optional.empty(),
              NodeMother.POSITION,
              NOW);

      assertThat(node.getId()).isEqualTo(NodeMother.NODE_ID);
      assertThat(node.getName()).isEqualTo(NodeMother.NAME);
      assertThat(node.getDescription()).isEqualTo(NodeMother.DESCRIPTION);
      assertThat(node.getParentId()).isEmpty();
      assertThat(node.getPosition()).isEqualTo(NodeMother.POSITION);
      assertThat(node.getCreatedAt()).isEqualTo(NOW);
      assertThat(node.getUpdatedAt()).isEqualTo(NOW);
      assertThat(node.getDeletedAt()).isEmpty();
    }

    @Test
    @DisplayName("should create node without a description")
    void should_create_node_without_a_description() {
      var node =
          Node.create(
              NodeMother.NODE_ID,
              NodeMother.NAME,
              null,
              Optional.empty(),
              NodeMother.POSITION,
              NOW);

      assertThat(node.getDescription()).isNull();
    }

    @Test
    @DisplayName("should create node under an existing parent")
    void should_create_node_under_an_existing_parent() {
      var parent = NodeMother.aNode().build();
      var child =
          Node.create(
              NodeMother.CHILD_ID,
              "child node",
              null,
              Optional.of(parent.getId()),
              NodeMother.POSITION,
              NOW);

      assertThat(child.getParentId()).contains(parent.getId());
    }

    @Test
    @DisplayName("should reject null id")
    void should_reject_null_id() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.create(
                      null, NodeMother.NAME, null, Optional.empty(), NodeMother.POSITION, NOW));
    }

    @Test
    @DisplayName("should reject null name")
    void should_reject_null_name() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.create(
                      NodeMother.NODE_ID, null, null, Optional.empty(), NodeMother.POSITION, NOW));
    }

    @Test
    @DisplayName("should reject blank name")
    void should_reject_blank_name() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  Node.create(
                      NodeMother.NODE_ID, "   ", null, Optional.empty(), NodeMother.POSITION, NOW));
    }

    @Test
    @DisplayName("should reject null parentId")
    void should_reject_null_parent_id() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.create(
                      NodeMother.NODE_ID, NodeMother.NAME, null, null, NodeMother.POSITION, NOW));
    }

    @Test
    @DisplayName("should reject self-referential parent")
    void should_reject_self_referential_parent() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  Node.create(
                      NodeMother.NODE_ID,
                      NodeMother.NAME,
                      null,
                      Optional.of(NodeMother.NODE_ID),
                      NodeMother.POSITION,
                      NOW));
    }

    @Test
    @DisplayName("should reject null position")
    void should_reject_null_position() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.create(
                      NodeMother.NODE_ID, NodeMother.NAME, null, Optional.empty(), null, NOW));
    }

    @Test
    @DisplayName("should reject null now")
    void should_reject_null_now() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.create(
                      NodeMother.NODE_ID,
                      NodeMother.NAME,
                      null,
                      Optional.empty(),
                      NodeMother.POSITION,
                      null));
    }
  }

  @Nested
  @DisplayName("Node.rename")
  class Rename {

    @Test
    @DisplayName("should return new node with new name, stamped updatedAt and original fields")
    void should_return_new_node_with_new_name_stamped_updated_at_and_original_fields() {
      var original = NodeMother.aChildNode().status(Node.Status.DONE).deleted().build();
      var newName = "new name";

      var renamed = original.rename(newName, LATER);

      assertThat(renamed.getName()).isEqualTo(newName);
      assertThat(renamed.getUpdatedAt()).isEqualTo(LATER);

      assertThat(renamed)
          .usingRecursiveComparison()
          .ignoringFields("name", "updatedAt")
          .isEqualTo(original);
    }

    @Test
    @DisplayName("should leave original node untouched")
    void should_leave_original_node_untouched() {
      var node = NodeMother.aNode().build();

      node.rename("new name", LATER);

      assertThat(node.getName()).isEqualTo(NodeMother.NAME);
      assertThat(node.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT);
    }

    @Test
    @DisplayName("should reject null name")
    void should_reject_null_name() {
      var node = NodeMother.aNode().build();

      assertThatNullPointerException().isThrownBy(() -> node.rename(null, LATER));
    }

    @Test
    @DisplayName("should reject blank name")
    void should_reject_blank_name() {
      var node = NodeMother.aNode().build();

      assertThatIllegalArgumentException().isThrownBy(() -> node.rename("   ", LATER));
    }

    @Test
    @DisplayName("should reject null now")
    void should_reject_null_now() {
      var node = NodeMother.aNode().build();

      assertThatNullPointerException().isThrownBy(() -> node.rename("new name", null));
    }

    @Test
    @DisplayName("should return same instance when name unchanged")
    void should_return_same_instance_when_name_unchanged() {
      var original = NodeMother.aNode().build();

      var renamed = original.rename(NodeMother.NAME, LATER);

      assertThat(renamed).isSameAs(original);
      assertThat(renamed.getName()).isEqualTo(NodeMother.NAME);
      assertThat(renamed.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT);
    }
  }

  @Nested
  @DisplayName("Node.changeDescription")
  class ChangeDescription {

    @Test
    @DisplayName(
        "should return new node with new description, stamped updatedAt and original fields")
    void should_return_new_node_with_new_description_stamped_updated_at_and_original_fields() {
      var original = NodeMother.aChildNode().status(Node.Status.DONE).deleted().build();
      var newDescription = "new description";

      var changed = original.changeDescription(newDescription, LATER);

      assertThat(changed.getDescription()).isEqualTo(newDescription);
      assertThat(changed.getUpdatedAt()).isEqualTo(LATER);

      assertThat(changed)
          .usingRecursiveComparison()
          .ignoringFields("description", "updatedAt")
          .isEqualTo(original);
    }

    @Test
    @DisplayName("should leave original node untouched")
    void should_leave_original_node_untouched() {
      var node = NodeMother.aNode().build();

      node.changeDescription("new description", LATER);

      assertThat(node.getDescription()).isEqualTo(NodeMother.DESCRIPTION);
      assertThat(node.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT);
    }

    @Test
    @DisplayName("should reject null now")
    void should_reject_null_now() {
      var node = NodeMother.aNode().build();

      assertThatNullPointerException()
          .isThrownBy(() -> node.changeDescription(NodeMother.DESCRIPTION, null));
    }

    @Test
    @DisplayName("should clear description when set to null")
    void should_clear_description_when_set_to_null() {
      var original = NodeMother.aNode().build();

      var changed = original.changeDescription(null, LATER);

      assertThat(changed.getDescription()).isNull();
      assertThat(changed.getUpdatedAt()).isEqualTo(LATER);
    }

    @Test
    @DisplayName("should return same instance when description unchanged")
    void should_return_same_instance_when_description_unchanged() {
      var original = NodeMother.aNode().build();

      var changed = original.changeDescription(NodeMother.DESCRIPTION, LATER);

      assertThat(changed).isSameAs(original);
      assertThat(changed.getDescription()).isEqualTo(NodeMother.DESCRIPTION);
      assertThat(changed.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT);
    }
  }

  @Nested
  @DisplayName("Node.withStatus")
  class WithStatus {

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("should accept every effective transition")
    @CsvSource(
        nullValues = "untracked",
        value = {
          "untracked, OPEN",
          "untracked, IN_PROGRESS",
          "untracked, DONE",
          "OPEN, untracked",
          "OPEN, IN_PROGRESS",
          "OPEN, DONE",
          "IN_PROGRESS, untracked",
          "IN_PROGRESS, OPEN",
          "IN_PROGRESS, DONE",
          "DONE, untracked",
          "DONE, OPEN",
          "DONE, IN_PROGRESS",
        })
    void should_accept_every_effective_transition(Node.Status from, Node.Status to) {
      var original = NodeMother.aChildNode().deleted().status(from).build();

      var updated = original.withStatus(Optional.ofNullable(to), LATER);

      assertThat(updated).isNotSameAs(original);
      assertThat(updated.getStatus()).isEqualTo(Optional.ofNullable(to));
      assertThat(updated.getUpdatedAt()).isEqualTo(LATER);
      assertThat(updated)
          .usingRecursiveComparison()
          .ignoringFields("status", "completedAt", "updatedAt")
          .isEqualTo(original);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("should return same instance when status unchanged")
    @CsvSource(
        nullValues = "untracked",
        value = {"untracked", "OPEN", "IN_PROGRESS", "DONE"})
    void should_return_same_instance_when_status_unchanged(Node.Status status) {
      var original = NodeMother.aNode().status(status).build();

      var updated = original.withStatus(Optional.ofNullable(status), LATER);

      assertThat(updated).isSameAs(original);
      assertThat(updated.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT);
    }

    @ParameterizedTest(name = "{0} -> DONE")
    @DisplayName("should set completedAt when entering DONE")
    @CsvSource(
        nullValues = "untracked",
        value = {"untracked", "OPEN", "IN_PROGRESS"})
    void should_set_completed_at_when_entering_done(Node.Status from) {
      var original = NodeMother.aNode().status(from).build();

      var updated = original.withStatus(Optional.of(Node.Status.DONE), LATER);

      assertThat(updated.getCompletedAt()).hasValue(LATER);
    }

    @ParameterizedTest(name = "DONE -> {0}")
    @DisplayName("should clear completedAt when leaving DONE")
    @CsvSource(
        nullValues = "untracked",
        value = {"untracked", "OPEN", "IN_PROGRESS"})
    void should_clear_completed_at_when_leaving_done(Node.Status to) {
      var original = NodeMother.aNode().status(Node.Status.DONE).build();
      assertThat(original.getCompletedAt()).isNotEmpty();

      var updated = original.withStatus(Optional.ofNullable(to), LATER);

      assertThat(updated.getCompletedAt()).isEmpty();
    }

    @Test
    @DisplayName("should leave original node untouched")
    void should_leave_original_node_untouched() {
      var node = NodeMother.aNode().status(Node.Status.OPEN).build();

      node.withStatus(Optional.of(Node.Status.DONE), LATER);

      assertThat(node.getStatus()).hasValue(Node.Status.OPEN);
      assertThat(node.getCompletedAt()).isEmpty();
      assertThat(node.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT);
    }

    @Test
    @DisplayName("should reject null status")
    void should_reject_null_status() {
      var node = NodeMother.aNode().build();

      assertThatNullPointerException().isThrownBy(() -> node.withStatus(null, LATER));
    }

    @Test
    @DisplayName("should reject null now")
    void should_reject_null_now() {
      var node = NodeMother.aNode().build();

      assertThatNullPointerException()
          .isThrownBy(() -> node.withStatus(Optional.of(Node.Status.DONE), null));
    }
  }

  @Nested
  @DisplayName("Node.reconstitute")
  class Reconstitute {

    @Test
    @DisplayName("should rebuild root node with explicit timestamps")
    void should_rebuild_root_node_with_explicit_timestamps() {
      var node =
          Node.reconstitute(
              NodeMother.NODE_ID,
              NodeMother.NAME,
              NodeMother.DESCRIPTION,
              null,
              NodeMother.POSITION,
              NodeMother.CREATED_AT,
              NodeMother.UPDATED_AT,
              NodeMother.DELETED_AT,
              Node.Status.DONE,
              NodeMother.COMPLETED_AT);

      assertThat(node.getId()).isEqualTo(NodeMother.NODE_ID);
      assertThat(node.getName()).isEqualTo(NodeMother.NAME);
      assertThat(node.getDescription()).isEqualTo(NodeMother.DESCRIPTION);
      assertThat(node.getParentId()).isEmpty();
      assertThat(node.getPosition()).isEqualTo(NodeMother.POSITION);
      assertThat(node.getCreatedAt()).isEqualTo(NodeMother.CREATED_AT);
      assertThat(node.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT);
      assertThat(node.getDeletedAt()).hasValue(NodeMother.DELETED_AT);
      assertThat(node.getStatus()).hasValue(Node.Status.DONE);
      assertThat(node.getCompletedAt()).hasValue(NodeMother.COMPLETED_AT);
    }

    @Test
    @DisplayName("should rebuild child node with parent")
    void should_rebuild_child_node_with_parent() {
      var parent = NodeMother.aNode().build();
      var child =
          Node.reconstitute(
              NodeMother.CHILD_ID,
              NodeMother.NAME,
              null,
              parent.getId(),
              NodeMother.POSITION,
              NodeMother.CREATED_AT,
              NodeMother.UPDATED_AT,
              null,
              null,
              null);

      assertThat(child.getParentId()).contains(parent.getId());
    }

    @Test
    @DisplayName("should reject null id")
    void should_reject_null_id() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.reconstitute(
                      null,
                      NodeMother.NAME,
                      null,
                      null,
                      NodeMother.POSITION,
                      NodeMother.CREATED_AT,
                      NodeMother.UPDATED_AT,
                      null,
                      null,
                      null));
    }

    @Test
    @DisplayName("should reject null name")
    void should_reject_null_name() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.reconstitute(
                      NodeMother.NODE_ID,
                      null,
                      null,
                      null,
                      NodeMother.POSITION,
                      NodeMother.CREATED_AT,
                      NodeMother.UPDATED_AT,
                      null,
                      null,
                      null));
    }

    @Test
    @DisplayName("should reject blank name")
    void should_reject_blank_name() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  Node.reconstitute(
                      NodeMother.NODE_ID,
                      "   ",
                      null,
                      null,
                      NodeMother.POSITION,
                      NodeMother.CREATED_AT,
                      NodeMother.UPDATED_AT,
                      null,
                      null,
                      null));
    }

    @Test
    @DisplayName("should reject self-referential parent")
    void should_reject_self_referential_parent() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  Node.reconstitute(
                      NodeMother.NODE_ID,
                      NodeMother.NAME,
                      null,
                      NodeMother.NODE_ID,
                      NodeMother.POSITION,
                      NodeMother.CREATED_AT,
                      NodeMother.UPDATED_AT,
                      null,
                      null,
                      null));
    }

    @Test
    @DisplayName("should reject null position")
    void should_reject_null_position() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.reconstitute(
                      NodeMother.NODE_ID,
                      NodeMother.NAME,
                      null,
                      null,
                      null,
                      NodeMother.CREATED_AT,
                      NodeMother.UPDATED_AT,
                      null,
                      null,
                      null));
    }

    @Test
    @DisplayName("should reject null createdAt")
    void should_reject_null_created_at() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.reconstitute(
                      NodeMother.NODE_ID,
                      NodeMother.NAME,
                      null,
                      null,
                      NodeMother.POSITION,
                      null,
                      NodeMother.UPDATED_AT,
                      null,
                      null,
                      null));
    }

    @Test
    @DisplayName("should reject null updatedAt")
    void should_reject_null_updated_at() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  Node.reconstitute(
                      NodeMother.NODE_ID,
                      NodeMother.NAME,
                      null,
                      null,
                      NodeMother.POSITION,
                      NodeMother.CREATED_AT,
                      null,
                      null,
                      null,
                      null));
    }
  }

  @Nested
  @DisplayName("Node equality")
  class Equality {

    @Test
    @DisplayName("should be equal to another node with same id")
    void should_be_equal_to_another_node_with_same_id() {
      var node = NodeMother.aNode().build();
      var otherNode = NodeMother.aNode().name("other name").build();

      assertThat(node).isEqualTo(otherNode);
    }

    @Test
    @DisplayName("should have same hashCode as another node with same id")
    void should_have_same_hash_code_as_another_node_with_same_id() {
      var node = NodeMother.aNode().build();
      var otherNode = NodeMother.aNode().name("other name").build();

      assertThat(node).hasSameHashCodeAs(otherNode);
    }

    @Test
    @DisplayName("should not be equal to node with different id")
    void should_not_be_equal_to_node_with_different_id() {
      var node = NodeMother.aNode().build();
      var otherNode = NodeMother.aNode().id(NodeId.of("other-node")).build();

      assertThat(node).isNotEqualTo(otherNode);
    }
  }
}
