package dev.nesto.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Node {

  private final NodeId id;
  private String name;
  private String description;

  @Getter(AccessLevel.NONE)
  private NodeId parentId;

  private Position position;

  private final Instant createdAt;
  private Instant updatedAt;

  private Node(NodeId id, String name, String description, NodeId parentId, Position position, Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.parentId = parentId;
    this.position = position;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Optional<NodeId> getParentId() {
    return Optional.ofNullable(parentId);
  }

  public static Node create(NodeId id, String name, String description, Optional<NodeId> parentId, Position position,
      Instant now) {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(parentId, "parentId must not be null");
    Objects.requireNonNull(position, "position must not be null");
    Objects.requireNonNull(now, "now must not be null");

    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }

    if (parentId.isPresent() && parentId.get().equals(id)) {
      throw new IllegalArgumentException("parentId must not equal id");
    }

    return new Node(id, name, description, parentId.orElse(null), position, now, now);
  }

  public void rename(String newName, Instant now) {
    Objects.requireNonNull(newName, "name must not be null");
    Objects.requireNonNull(now, "now must not be null");
    if (newName.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }

    if (Objects.equals(newName, this.name)) {
      return;
    }

    this.name = newName;
    this.updatedAt = now;
  }

  public void changeDescription(String newDescription, Instant now) {
    Objects.requireNonNull(now, "now must not be null");

    if (Objects.equals(newDescription, this.description)) {
      return;
    }

    this.description = newDescription;
    this.updatedAt = now;
  }
}
