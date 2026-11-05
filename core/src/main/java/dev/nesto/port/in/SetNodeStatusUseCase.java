package dev.nesto.port.in;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import java.util.Objects;
import java.util.Optional;

public interface SetNodeStatusUseCase {

  void setNodeStatus(Command command);

  // Empty status means untracked, as in Node.withStatus
  record Command(NodeId nodeId, Optional<Node.Status> status) {

    public Command {
      Objects.requireNonNull(nodeId, "nodeId must not be null");
      Objects.requireNonNull(status, "status must not be null");
    }
  }
}
