package dev.nesto.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import dev.nesto.adapter.out.persistence.NodeInMemoryRepository;
import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.NodeMother;
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

  private NodeInMemoryRepository repository;
  private NodeApplicationService service;

  @BeforeEach
  void setUp() {
    repository = new NodeInMemoryRepository();
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
          .hasValueSatisfying(
              n -> {
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
          .hasValueSatisfying(
              n -> {
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
          .hasValueSatisfying(
              n -> {
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
  }
}
