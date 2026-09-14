package dev.nesto.port.out;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import java.util.List;
import java.util.Optional;

public interface NodeRepositoryPort {

  void save(Node node);

  Optional<Node> findById(NodeId id);

  List<Node> findRoots();

  List<Node> findChildren(NodeId parentId);
}
