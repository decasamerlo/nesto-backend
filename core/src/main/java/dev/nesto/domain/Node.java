package dev.nesto.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Node {

  @EqualsAndHashCode.Include private final NodeId id;
  private final String name;
  private final String description;

  @Getter(AccessLevel.NONE)
  private final NodeId parentId;

  private final Position position;

  private final Instant createdAt;
  private final Instant updatedAt;

  private Node(
      NodeId id,
      String name,
      String description,
      NodeId parentId,
      Position position,
      Instant createdAt,
      Instant updatedAt) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.name = Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    this.description = description;
    this.parentId = parentId;
    this.position = Objects.requireNonNull(position, "position must not be null");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    if (parentId != null && parentId.equals(id)) {
      throw new IllegalArgumentException("parentId must not equal id");
    }
  }

  public Optional<NodeId> getParentId() {
    return Optional.ofNullable(parentId);
  }

  public static Node create(
      NodeId id,
      String name,
      String description,
      Optional<NodeId> parentId,
      Position position,
      Instant now) {
    Objects.requireNonNull(parentId, "parentId must not be null");
    Objects.requireNonNull(now, "now must not be null");

    return new Node(id, name, description, parentId.orElse(null), position, now, now);
  }

  public Node rename(String newName, Instant now) {
    Objects.requireNonNull(newName, "name must not be null");
    Objects.requireNonNull(now, "now must not be null");
    if (newName.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }

    if (Objects.equals(newName, this.name)) {
      return this;
    }

    return copyWith(newName, this.description, now);
  }

  public Node changeDescription(String newDescription, Instant now) {
    Objects.requireNonNull(now, "now must not be null");

    if (Objects.equals(newDescription, this.description)) {
      return this;
    }

    return copyWith(this.name, newDescription, now);
  }

  public static Node reconstitute(
      NodeId id,
      String name,
      String description,
      NodeId parentId,
      Position position,
      Instant createdAt,
      Instant updatedAt) {

    return new Node(id, name, description, parentId, position, createdAt, updatedAt);
  }

  private Node copyWith(String name, String description, Instant updatedAt) {
    return new Node(
        this.id, name, description, this.parentId, this.position, this.createdAt, updatedAt);
  }
}
