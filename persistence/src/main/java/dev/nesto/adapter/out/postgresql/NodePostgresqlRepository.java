package dev.nesto.adapter.out.postgresql;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.port.out.NodeRepositoryPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Not @Repository: its exception translation would rewrap the IllegalStateException and
// IllegalArgumentException thrown below as Spring's InvalidDataAccessApiUsageException.
// @DataJpaTest doesn't load that translation, so only a full application context shows it
@Component
@AllArgsConstructor
class NodePostgresqlRepository implements NodeRepositoryPort {

  private final SpringDataNodeRepository springData;
  private final NodePersistenceMapper mapper;

  @Override
  public void save(Node node) {

    if (node.getDeletedAt().isPresent()) {
      throw new IllegalArgumentException(
          "node %s carries a deletedAt; delete through softDeleteSubtree, not save"
              .formatted(node.getId().value()));
    }

    int affected;

    try {
      affected =
          springData.upsert(
              node.getId().value(),
              node.getName(),
              node.getDescription(),
              node.getParentId().map(NodeId::value).orElse(null),
              node.getPosition().value(),
              node.getCreatedAt(),
              node.getUpdatedAt(),
              node.getStatus().map(Enum::name).orElse(null),
              node.getCompletedAt().orElse(null));
    } catch (DataIntegrityViolationException e) {
      throw new IllegalStateException(
          "node %s violates a database constraint".formatted(node.getId().value()), e);
    }

    // Zero rows: the row exists and is soft-deleted, so the upsert's WHERE node.deleted_at IS NULL
    // skipped the DO UPDATE
    if (affected == 0) {
      throw new IllegalStateException(
          "node %s is soft-deleted; restore it before saving".formatted(node.getId().value()));
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Node> findById(NodeId id) {
    return springData.findById(id.value()).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Node> findRoots() {
    return toDomainList(springData.findByParentIdIsNullOrderByPositionAsc());
  }

  @Override
  @Transactional(readOnly = true)
  public List<Node> findChildren(NodeId parentId) {
    return toDomainList(springData.findByParentIdOrderByPositionAsc(parentId.value()));
  }

  @Override
  public int softDeleteSubtree(NodeId rootId, Instant deletedAt) {
    return springData.softDeleteSubtree(rootId.value(), deletedAt);
  }

  @Override
  public int restoreSubtree(NodeId rootId, Instant deletedAt) {
    return springData.restoreSubtree(rootId.value(), deletedAt);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Node> findDeletedById(NodeId id) {
    return springData.findDeletedById(id.value()).map(mapper::toDomain);
  }

  // Mutable on purpose: callers may modify the returned list (the contract's assertDefensiveCopy
  // clears it), so not Stream.toList() or List.copyOf
  private List<Node> toDomainList(List<NodeEntity> entities) {
    return entities.stream().map(mapper::toDomain).collect(Collectors.toCollection(ArrayList::new));
  }
}
