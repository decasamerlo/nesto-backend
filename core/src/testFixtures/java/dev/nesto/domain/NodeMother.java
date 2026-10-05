package dev.nesto.domain;

import java.time.Instant;

/**
 * Fluent builder for test nodes. Every default is deliberately distinct, see the note below before
 * changing one
 */
public final class NodeMother {

  public static final NodeId NODE_ID = NodeId.of("node");
  public static final NodeId CHILD_ID = NodeId.of("child");
  public static final String NAME = "node name";
  public static final String DESCRIPTION = "node description";

  public static final Position POSITION = Position.of(3);

  // In chronological order, DELETED_AT is always last: tests derive their operation instants from
  // this one, so a new instant goes before it
  public static final Instant CREATED_AT = Instant.parse("2026-01-01T01:00:00Z");
  public static final Instant UPDATED_AT = Instant.parse("2026-01-02T01:00:00Z");
  public static final Instant COMPLETED_AT = Instant.parse("2026-01-03T01:00:00Z");
  public static final Instant DELETED_AT = Instant.parse("2026-01-04T01:00:00Z");

  private NodeId id = NODE_ID;
  private String name = NAME;
  private String description = DESCRIPTION;
  private NodeId parentId = null;
  private Position position = POSITION;
  private Instant createdAt = CREATED_AT;
  private Instant updatedAt = UPDATED_AT;
  private Instant deletedAt = null;
  private Node.Status status = null;
  private Instant completedAt = null;

  private NodeMother() {}

  public static NodeMother aNode() {
    return new NodeMother();
  }

  public static NodeMother aChildNode() {
    return aNode().id(CHILD_ID).parentId(NODE_ID);
  }

  public NodeMother id(NodeId value) {
    this.id = value;
    return this;
  }

  public NodeMother name(String value) {
    this.name = value;
    return this;
  }

  public NodeMother parentId(NodeId value) {
    this.parentId = value;
    return this;
  }

  public NodeMother position(Position value) {
    this.position = value;
    return this;
  }

  public NodeMother status(Node.Status value) {
    this.status = value;
    this.completedAt = Node.Status.DONE.equals(value) ? COMPLETED_AT : null;
    return this;
  }

  /**
   * For in-memory domain tests only. Repository tests delete through softDeleteSubtree: save()
   * rejects a node carrying a deletedAt
   */
  public NodeMother deleted() {
    this.deletedAt = DELETED_AT;
    return this;
  }

  public Node build() {
    return Node.reconstitute(
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
}
