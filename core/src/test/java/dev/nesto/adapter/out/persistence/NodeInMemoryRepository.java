package dev.nesto.adapter.out.persistence;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.port.out.NodeRepositoryPort;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class NodeInMemoryRepository implements NodeRepositoryPort {

  private static final Predicate<Node> ACTIVE = node -> node.getDeletedAt().isEmpty();
  private final Map<NodeId, Node> nodes = new HashMap<>();

  @Override
  public void save(Node node) {
    nodes.put(node.getId(), node);
  }

  @Override
  public Optional<Node> findById(NodeId id) {
    return Optional.ofNullable(nodes.get(id)).filter(ACTIVE);
  }

  @Override
  public List<Node> findRoots() {
    return sortedByPosition(ACTIVE.and(n -> n.getParentId().isEmpty()));
  }

  @Override
  public List<Node> findChildren(NodeId parentId) {
    return sortedByPosition(ACTIVE.and(n -> n.getParentId().filter(parentId::equals).isPresent()));
  }

  @Override
  public int softDeleteSubtree(NodeId rootId, Instant deletedAt) {
    List<Node> toStamp = subtreeOf(rootId).stream().filter(ACTIVE).toList();
    toStamp.forEach(node -> save(withDeletedAt(node, deletedAt)));
    return toStamp.size();
  }

  @Override
  public int restoreSubtree(NodeId rootId, Instant deletedAt) {
    List<Node> toClear =
        subtreeOf(rootId).stream()
            .filter(n -> n.getDeletedAt().filter(deletedAt::equals).isPresent())
            .toList();
    toClear.forEach(node -> save(withDeletedAt(node, null)));
    return toClear.size();
  }

  @Override
  public Optional<Node> findDeletedById(NodeId id) {
    return Optional.ofNullable(nodes.get(id)).filter(ACTIVE.negate());
  }

  private List<Node> sortedByPosition(Predicate<Node> predicate) {
    return nodes.values().stream()
        .filter(predicate)
        .sorted(Comparator.comparing(Node::getPosition))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<Node> allChildrenOf(NodeId parentId) {
    return nodes.values().stream()
        .filter(node -> node.getParentId().filter(parentId::equals).isPresent())
        .toList();
  }

  private List<Node> subtreeOf(NodeId rootId) {
    Node root = nodes.get(rootId);
    if (root == null) {
      return List.of();
    }

    List<Node> subtree = new ArrayList<>();
    Queue<Node> pending = new ArrayDeque<>();
    pending.add(root);

    while (!pending.isEmpty()) {
      Node current = pending.remove();
      subtree.add(current);
      pending.addAll(allChildrenOf(current.getId()));
    }

    return subtree;
  }

  private static Node withDeletedAt(Node node, Instant deletedAt) {
    return Node.reconstitute(
        node.getId(),
        node.getName(),
        node.getDescription(),
        node.getParentId().orElse(null),
        node.getPosition(),
        node.getCreatedAt(),
        node.getUpdatedAt(),
        deletedAt);
  }
}
