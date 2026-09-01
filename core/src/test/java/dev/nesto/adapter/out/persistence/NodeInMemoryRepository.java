package dev.nesto.adapter.out.persistence;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.port.out.NodeRepositoryPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NodeInMemoryRepository implements NodeRepositoryPort {

  private final Map<NodeId, Node> nodes = new HashMap<>();

  @Override
  public void save(Node node) {
    nodes.put(node.getId(), node);
  }

  @Override
  public Optional<Node> findById(NodeId id) {
    return Optional.ofNullable(nodes.get(id));
  }

  @Override
  public List<Node> findRoots() {
    return sortedByPosition(n -> n.getParentId().isEmpty());
  }

  @Override
  public List<Node> findChildren(NodeId parentId) {
    return sortedByPosition(n -> n.getParentId().filter(parentId::equals).isPresent());
  }

  private List<Node> sortedByPosition(Predicate<Node> predicate) {
    return nodes.values().stream()
        .filter(predicate)
        .sorted(Comparator.comparing(Node::getPosition))
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
