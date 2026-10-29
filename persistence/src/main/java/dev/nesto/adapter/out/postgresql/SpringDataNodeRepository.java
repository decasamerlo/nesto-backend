package dev.nesto.adapter.out.postgresql;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Transactional
interface SpringDataNodeRepository extends JpaRepository<NodeEntity, String> {

  // --- derived: @SQLRestriction applies, so none of these says deleted_at ---

  List<NodeEntity> findByParentIdIsNullOrderByPositionAsc();

  List<NodeEntity> findByParentIdOrderByPositionAsc(String parentId);

  // --- native: @SQLRestriction does not apply, so each one says it ---

  @Modifying(clearAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO node (id, name, description, parent_id, position, created_at, updated_at, status, completed_at)
          VALUES (:id, :name, :description, :parentId, :position, :createdAt, :updatedAt, :status, :completedAt)
          ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description, parent_id = EXCLUDED.parent_id,
            position = EXCLUDED.position, updated_at = EXCLUDED.updated_at, status = EXCLUDED.status, completed_at = EXCLUDED.completed_at
            WHERE node.deleted_at IS NULL
          """,
      nativeQuery = true)
  int upsert(
      @Param("id") String id,
      @Param("name") String name,
      @Param("description") String description,
      @Param("parentId") String parentId,
      @Param("position") int position,
      @Param("createdAt") Instant createdAt,
      @Param("updatedAt") Instant updatedAt,
      @Param("status") String status,
      @Param("completedAt") Instant completedAt);

  @Modifying(clearAutomatically = true)
  @Query(
      value =
          """
          WITH RECURSIVE subtree AS (
            SELECT id FROM node WHERE id = :id
            UNION
            SELECT n.id FROM node n JOIN subtree s ON n.parent_id = s.id
          )
          UPDATE node
          SET deleted_at = :deletedAt
          WHERE id IN (SELECT id FROM subtree) AND deleted_at IS NULL
          """,
      nativeQuery = true)
  int softDeleteSubtree(@Param("id") String id, @Param("deletedAt") Instant deletedAt);

  @Modifying(clearAutomatically = true)
  @Query(
      value =
          """
          WITH RECURSIVE subtree AS (
            SELECT id FROM node WHERE id = :id
            UNION
            SELECT n.id FROM node n JOIN subtree s ON n.parent_id = s.id
          )
          UPDATE node
          SET deleted_at = null
          WHERE id IN (SELECT id FROM subtree) AND deleted_at = :deletedAt
          """,
      nativeQuery = true)
  int restoreSubtree(@Param("id") String id, @Param("deletedAt") Instant deletedAt);

  @Query(value = "SELECT * FROM node WHERE id = :id AND deleted_at IS NOT NULL", nativeQuery = true)
  Optional<NodeEntity> findDeletedById(@Param("id") String id);
}
