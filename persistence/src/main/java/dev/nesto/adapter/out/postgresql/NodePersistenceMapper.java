package dev.nesto.adapter.out.postgresql;

import dev.nesto.domain.Node;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.Position;
import org.springframework.stereotype.Component;

@Component
class NodePersistenceMapper {

  Node toDomain(NodeEntity entity) {
    return Node.reconstitute(
        NodeId.of(entity.getId()),
        entity.getName(),
        entity.getDescription(),
        entity.getParentId() != null ? NodeId.of(entity.getParentId()) : null,
        Position.of(entity.getPosition()),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getDeletedAt(),
        entity.getStatus(),
        entity.getCompletedAt());
  }
}
