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

  public enum Status {
    OPEN,
    IN_PROGRESS,
    DONE
  }

  @EqualsAndHashCode.Include private final NodeId id;
  private final String name;
  private final String description;

  @Getter(AccessLevel.NONE)
  private final NodeId parentId;

  private final Position position;

  private final Instant createdAt;
  private final Instant updatedAt;

  @Getter(AccessLevel.NONE)
  private final Instant deletedAt;

  @Getter(AccessLevel.NONE)
  private final Status status;

  @Getter(AccessLevel.NONE)
  private final Instant completedAt;

  private Node(
      NodeId id,
      String name,
      String description,
      NodeId parentId,
      Position position,
      Instant createdAt,
      Instant updatedAt,
      Instant deletedAt,
      Status status,
      Instant completedAt) {
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
    this.deletedAt = deletedAt;
    this.status = status;
    this.completedAt = completedAt;
    if (parentId != null && parentId.equals(id)) {
      throw new IllegalArgumentException("parentId must not equal id");
    }
  }

  public Optional<NodeId> getParentId() {
    return Optional.ofNullable(parentId);
  }

  public Optional<Instant> getDeletedAt() {
    return Optional.ofNullable(deletedAt);
  }

  public Optional<Status> getStatus() {
    return Optional.ofNullable(status);
  }

  public Optional<Instant> getCompletedAt() {
    return Optional.ofNullable(completedAt);
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

    return new Node(
        id, name, description, parentId.orElse(null), position, now, now, null, null, null);
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

  public Node withStatus(Optional<Status> newStatus, Instant now) {
    Objects.requireNonNull(newStatus, "status must not be null");
    Objects.requireNonNull(now, "now must not be null");

    Status target = newStatus.orElse(null);

    if (Objects.equals(target, this.status)) {
      return this;
    }

    return copyWithStatus(target, Status.DONE.equals(target) ? now : null, now);
  }

  public static Node reconstitute(
      NodeId id,
      String name,
      String description,
      NodeId parentId,
      Position position,
      Instant createdAt,
      Instant updatedAt,
      Instant deletedAt,
      Status status,
      Instant completedAt) {

    return new Node(
        id,
        name,
        description,
        parentId,
        position,
        createdAt,
        updatedAt,
        deletedAt,
        status,
        completedAt);
  }

  private Node copyWith(String name, String description, Instant updatedAt) {
    return new Node(
        this.id,
        name,
        description,
        this.parentId,
        this.position,
        this.createdAt,
        updatedAt,
        this.deletedAt,
        this.status,
        this.completedAt);
  }

  private Node copyWithStatus(Status status, Instant completedAt, Instant updatedAt) {
    return new Node(
        this.id,
        this.name,
        this.description,
        this.parentId,
        this.position,
        this.createdAt,
        updatedAt,
        this.deletedAt,
        status,
        completedAt);
  }
}
