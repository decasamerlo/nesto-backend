package dev.nesto.application;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.port.in.SetNodeStatusUseCase;
import dev.nesto.port.out.NodeRepositoryPort;
import java.time.Clock;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NodeApplicationService implements SetNodeStatusUseCase {

  private final NodeRepositoryPort repository;
  private final Clock clock;

  @Override
  public void setNodeStatus(SetNodeStatusUseCase.Command command) {
    Node node = getNode(command.nodeId());
    repository.save(node.withStatus(command.status(), clock.instant()));
  }

  private Node getNode(NodeId id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("node %s not found".formatted(id.value())));
  }
}
