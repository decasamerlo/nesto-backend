package dev.nesto.application;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.port.in.GetNodeStatusSummaryQuery;
import dev.nesto.port.in.SetNodeStatusUseCase;
import dev.nesto.port.out.NodeRepositoryPort;
import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NodeApplicationService implements SetNodeStatusUseCase, GetNodeStatusSummaryQuery {

  private final NodeRepositoryPort repository;
  private final Clock clock;

  @Override
  public void setNodeStatus(SetNodeStatusUseCase.Command command) {
    Objects.requireNonNull(command, "command must not be null");
    Node node = getNode(command.nodeId());
    repository.save(node.withStatus(command.status(), clock.instant()));
  }

  @Override
  public GetNodeStatusSummaryQuery.Result getNodeStatusSummary(NodeId nodeId) {
    Objects.requireNonNull(nodeId, "nodeId must not be null");

    // Called for its throw alone: listChildren matches the parent id and never checks the parent,
    // so it answers for an absent or deleted node too
    getNode(nodeId);

    int done = 0;
    int inProgress = 0;
    int open = 0;
    int untracked = 0;

    for (Node child : repository.listChildren(nodeId)) {
      // No default: with case null the switch must cover every Status, so a fourth one fails to
      // compile here instead of going uncounted
      switch (child.getStatus().orElse(null)) {
        case DONE -> done++;
        case IN_PROGRESS -> inProgress++;
        case OPEN -> open++;
        case null -> untracked++;
      }
    }

    return new GetNodeStatusSummaryQuery.Result(done, inProgress, open, untracked);
  }

  private Node getNode(NodeId id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("node %s not found".formatted(id.value())));
  }
}
