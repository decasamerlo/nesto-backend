package dev.nesto.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import dev.nesto.adapter.out.persistence.inmemory.NodeInMemoryRepository;
import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.NodeMother;
import dev.nesto.domain.Position;
import dev.nesto.port.in.GetNodeStatusSummaryQuery;
import dev.nesto.port.in.SetNodeStatusUseCase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NodeApplicationServiceTest {

  private static final NodeId ABSENT_ID = NodeId.of("01JZ5Z9WX6X1Y45C7ZHYV8NQKD");
  private static final NodeId DELETED_ID = NodeId.of("01JZ60A2B3C4D5E6F7G8H9J0KM");

  // Later than every NodeMother instant by construction, so no fixture value can equal the clock's
  // reading
  private static final Instant NOW = NodeMother.DELETED_AT.plus(1, ChronoUnit.DAYS);

  private RecordingRepository repository;
  private NodeApplicationService service;

  @BeforeEach
  void setUp() {
    repository = new RecordingRepository();
    service = new NodeApplicationService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Nested
  @DisplayName("setNodeStatus")
  class SetNodeStatus {

    @Test
    @DisplayName("should set DONE on an untracked node, stamping completedAt and updatedAt")
    void should_set_done_on_untracked_node_stamping_completed_at_and_updated_at() {
      var node = NodeMother.aNode().build();
      repository.save(node);
      service.setNodeStatus(
          new SetNodeStatusUseCase.Command(node.getId(), Optional.of(Node.Status.DONE)));

      assertThat(repository.findById(node.getId()))
          .hasValueSatisfying(n -> {
            assertThat(n.getStatus()).hasValue(Node.Status.DONE);
            assertThat(n.getCompletedAt()).hasValue(NOW);
            assertThat(n.getUpdatedAt()).isEqualTo(NOW);
          });
    }

    @Test
    @DisplayName("should clear completedAt when DONE becomes OPEN")
    void should_clear_completed_at_when_done_becomes_open() {
      var node = NodeMother.aNode().status(Node.Status.DONE).build();
      repository.save(node);
      service.setNodeStatus(
          new SetNodeStatusUseCase.Command(node.getId(), Optional.of(Node.Status.OPEN)));

      assertThat(repository.findById(node.getId()))
          .hasValueSatisfying(n -> {
            assertThat(n.getStatus()).hasValue(Node.Status.OPEN);
            assertThat(n.getCompletedAt()).isEmpty();
          });
    }

    @Test
    @DisplayName("should untrack a DONE node, clearing completedAt")
    void should_untrack_done_node_clearing_completed_at() {
      var node = NodeMother.aNode().status(Node.Status.DONE).build();
      repository.save(node);
      service.setNodeStatus(new SetNodeStatusUseCase.Command(node.getId(), Optional.empty()));

      assertThat(repository.findById(node.getId()))
          .hasValueSatisfying(n -> {
            assertThat(n.getStatus()).isEmpty();
            assertThat(n.getCompletedAt()).isEmpty();
          });
    }

    @Test
    @DisplayName("should leave updatedAt untouched when the status is unchanged")
    void should_leave_updated_at_untouched_when_status_is_unchanged() {
      var node = NodeMother.aNode().status(Node.Status.IN_PROGRESS).build();
      repository.save(node);
      service.setNodeStatus(
          new SetNodeStatusUseCase.Command(node.getId(), Optional.of(Node.Status.IN_PROGRESS)));

      assertThat(repository.findById(node.getId()))
          .hasValueSatisfying(n -> assertThat(n.getUpdatedAt()).isEqualTo(NodeMother.UPDATED_AT));
    }

    @Test
    @DisplayName("should save even when the status is unchanged")
    void should_save_even_when_status_is_unchanged() {
      var node = NodeMother.aNode().status(Node.Status.IN_PROGRESS).build();
      repository.save(node);
      var savesBefore = repository.saves;

      service.setNodeStatus(
          new SetNodeStatusUseCase.Command(node.getId(), Optional.of(Node.Status.IN_PROGRESS)));

      assertThat(repository.saves).isEqualTo(savesBefore + 1);
    }

    @Test
    @DisplayName("should change only the commanded node")
    void should_change_only_commanded_node() {
      var parent = NodeMother.aNode().build();
      var child = NodeMother.aChildNode().build();
      repository.save(parent);
      repository.save(child);

      service.setNodeStatus(
          new SetNodeStatusUseCase.Command(child.getId(), Optional.of(Node.Status.IN_PROGRESS)));

      assertThat(repository.findById(child.getId()))
          .hasValueSatisfying(n -> assertThat(n.getStatus()).hasValue(Node.Status.IN_PROGRESS));
      assertThat(repository.findById(parent.getId()))
          .get()
          .usingRecursiveComparison()
          .isEqualTo(parent);
    }

    @Test
    @DisplayName("should reject an absent node, naming its id")
    void should_reject_absent_node_naming_id() {
      var command = new SetNodeStatusUseCase.Command(ABSENT_ID, Optional.of(Node.Status.DONE));

      assertThatExceptionOfType(NoSuchElementException.class)
          .isThrownBy(() -> service.setNodeStatus(command))
          .withMessageContaining(ABSENT_ID.value());
    }

    @Test
    @DisplayName("should reject a deleted node")
    void should_reject_deleted_node() {
      var node = NodeMother.aNode().id(DELETED_ID).build();
      repository.save(node);
      repository.softDeleteSubtree(node.getId(), NOW);
      var command = new SetNodeStatusUseCase.Command(node.getId(), Optional.of(Node.Status.DONE));

      assertThatExceptionOfType(NoSuchElementException.class)
          .isThrownBy(() -> service.setNodeStatus(command))
          .withMessageContaining(DELETED_ID.value());
    }

    @Test
    @DisplayName("should let save's rejection through for a node deleted after the load")
    void should_let_save_rejection_through_for_node_deleted_after_load() {
      var node = NodeMother.aNode().build();
      repository.save(node);
      repository.deleteBeforeSave = true;

      var command = new SetNodeStatusUseCase.Command(node.getId(), Optional.of(Node.Status.DONE));

      assertThatIllegalStateException().isThrownBy(() -> service.setNodeStatus(command));
    }

    @Test
    @DisplayName("should reject a null command")
    void should_reject_null_command() {
      // The exact message: the JDK's own NPE at command.nodeId() already names "command"
      assertThatNullPointerException()
          .isThrownBy(() -> service.setNodeStatus(null))
          .withMessage("command must not be null");
    }
  }

  @Nested
  @DisplayName("getNodeStatusSummary")
  class GetNodeStatusSummary {

    private final Node parent = NodeMother.aNode().build();
    private int nextChild;

    @BeforeEach
    void saveParent() {
      repository.save(parent);
    }

    private Node saveChild(NodeId parentId, Node.Status status) {
      nextChild++;
      var child =
          NodeMother.aNode()
              .id(NodeId.of("child-" + nextChild))
              .parentId(parentId)
              .position(Position.of(nextChild))
              .status(status)
              .build();
      repository.save(child);
      return child;
    }

    private void saveChildren(int count, Node.Status status) {
      for (int i = 0; i < count; i++) {
        saveChild(parent.getId(), status);
      }
    }

    @Test
    @DisplayName("should give four zeros for a node with no children")
    void should_give_four_zeros_for_node_with_no_children() {
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(0, 0, 0, 0));
    }

    @Test
    @DisplayName("should count children that are all untracked as untracked")
    void should_count_all_untracked_children_as_untracked() {
      saveChildren(3, null);

      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(0, 0, 0, 3));
    }

    @Test
    @DisplayName("should count each status into its own bucket")
    void should_count_each_status_into_its_own_bucket() {
      // A different count in every bucket, so a count that swaps two of them changes the Result
      saveChildren(1, Node.Status.DONE);
      saveChildren(2, Node.Status.IN_PROGRESS);
      saveChildren(3, Node.Status.OPEN);
      saveChildren(4, null);

      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(1, 2, 3, 4));
    }

    @Test
    @DisplayName("should reject an absent node, naming its id")
    void should_reject_absent_node_naming_id() {
      assertThatExceptionOfType(NoSuchElementException.class)
          .isThrownBy(() -> service.getNodeStatusSummary(ABSENT_ID))
          .withMessageContaining(ABSENT_ID.value());
    }

    @Test
    @DisplayName("should reject a deleted node, naming its id")
    void should_reject_deleted_node_naming_id() {
      repository.save(NodeMother.aNode().id(DELETED_ID).build());
      repository.softDeleteSubtree(DELETED_ID, NOW);

      assertThatExceptionOfType(NoSuchElementException.class)
          .isThrownBy(() -> service.getNodeStatusSummary(DELETED_ID))
          .withMessageContaining(DELETED_ID.value());
    }

    // A test that changes a child reads once before the change too, so a summary kept from an
    // earlier read fails it
    @Test
    @DisplayName("should follow a child's status change")
    void should_follow_child_status_change() {
      var child = saveChild(parent.getId(), Node.Status.OPEN);
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(0, 0, 1, 0));

      service.setNodeStatus(
          new SetNodeStatusUseCase.Command(child.getId(), Optional.of(Node.Status.DONE)));
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(1, 0, 0, 0));
    }

    @Test
    @DisplayName("should count a child added after a read")
    void should_count_child_added_after_read() {
      saveChildren(1, Node.Status.OPEN);
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(0, 0, 1, 0));

      saveChildren(1, Node.Status.IN_PROGRESS);
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(0, 1, 1, 0));
    }

    @Test
    @DisplayName("should leave out a deleted child")
    void should_leave_out_deleted_child() {
      saveChildren(1, Node.Status.DONE);
      var child = saveChild(parent.getId(), Node.Status.OPEN);
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(1, 0, 1, 0));

      repository.softDeleteSubtree(child.getId(), NOW);
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(1, 0, 0, 0));
    }

    @Test
    @DisplayName("should count a restored child again")
    void should_count_restored_child_again() {
      saveChildren(1, Node.Status.DONE);
      var child = saveChild(parent.getId(), Node.Status.OPEN);
      repository.softDeleteSubtree(child.getId(), NOW);
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(1, 0, 0, 0));

      repository.restoreSubtree(child.getId(), NOW);
      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(1, 0, 1, 0));
    }

    @Test
    @DisplayName("should not count a grandchild")
    void should_not_count_grandchild() {
      var child = saveChild(parent.getId(), Node.Status.DONE);
      // Not DONE like its parent, so counting grandchildren in place of children changes the Result
      saveChild(child.getId(), Node.Status.OPEN);

      assertThat(service.getNodeStatusSummary(parent.getId()))
          .isEqualTo(new GetNodeStatusSummaryQuery.Result(1, 0, 0, 0));
    }

    @Test
    @DisplayName("should reject a null nodeId")
    void should_reject_null_node_id() {
      assertThatNullPointerException()
          .isThrownBy(() -> service.getNodeStatusSummary(null))
          .withMessage("nodeId must not be null");
    }
  }

  // The fake, plus what these tests need around save: how many times it ran, and the deletion
  // ADR 011's race puts between the load and the save
  private static final class RecordingRepository extends NodeInMemoryRepository {

    private int saves;
    private boolean deleteBeforeSave;

    @Override
    public void save(Node node) {
      saves++;
      if (deleteBeforeSave) {
        softDeleteSubtree(node.getId(), NOW);
      }
      super.save(node);
    }
  }
}
