package dev.nesto.adapter.out.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import dev.nesto.domain.Node;
import dev.nesto.domain.Node.Status;
import dev.nesto.domain.NodeId;
import dev.nesto.domain.NodeMother;
import dev.nesto.port.out.NodeRepositoryContractTest;
import dev.nesto.port.out.NodeRepositoryPort;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(
    properties = {
      "spring.liquibase.change-log=classpath:db/postgresql/changelog/db.changelog-master.yaml",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresContainer.class, NodePostgresqlRepository.class, NodePersistenceMapper.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NodePostgresqlRepositoryTest extends NodeRepositoryContractTest {

  private static NodeRepositoryPort adapter;
  private static JdbcTemplate jdbcTemplate;

  // Static and injected in @BeforeAll: the contract's @Nested classes are declared in the abstract
  // base, so Spring builds their context from it and can't inject instance fields or a constructor
  // here
  @BeforeAll
  static void wire(@Autowired NodeRepositoryPort port, @Autowired JdbcTemplate jdbc) {
    adapter = port;
    jdbcTemplate = jdbc;
  }

  @Override
  protected NodeRepositoryPort createRepository() {
    jdbcTemplate.execute("TRUNCATE TABLE node");
    return adapter;
  }

  @Test
  @DisplayName("should round-trip every field")
  void should_round_trip_every_field() {
    repository.save(NodeMother.aNode().build());
    Node child = NodeMother.aChildNode().status(Status.DONE).build();

    repository.save(child);

    assertThat(repository.findById(child.getId()))
        .get()
        .usingRecursiveComparison()
        .isEqualTo(child);
  }

  @Test
  @DisplayName("should store every date column as instant")
  void should_store_every_date_column_as_instant() {
    var dateTypes =
        jdbcTemplate.queryForList(
            """
            SELECT data_type FROM information_schema.columns
            WHERE table_name = 'node'
              AND column_name IN ('created_at', 'updated_at', 'deleted_at', 'completed_at')
            """,
            String.class);

    assertThat(dateTypes).hasSize(4);
    assertThat(dateTypes).allMatch(dateType -> "timestamp with time zone".equals(dateType));
  }

  @Test
  @DisplayName("should persist and load non-ULID id")
  void should_persist_and_load_non_ulid_id() {
    // Longer than a ULID's 26 characters, so a column sized for ULIDs rejects it
    var id = NodeId.of("legacy/42 este nó tem um acento para teste");

    repository.save(NodeMother.aNode().id(id).build());

    assertThat(repository.findById(id).map(Node::getId)).contains(id);
  }
}
