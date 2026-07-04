package org.criticizer.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.criticizer.entity.User;
import org.criticizer.repository.GenreRepository;
import org.criticizer.repository.TagRepository;
import org.criticizer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The only test that exercises the real production stack: Flyway migrations against MySQL 8 plus
 * Hibernate schema validation. The H2-based tests build the schema from the entities, so a broken
 * migration or an entity/DDL mismatch would otherwise sail through CI unnoticed.
 *
 * <p>Skipped automatically when Docker is not available (local runs); always runs in CI.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        properties = {
            // validate is what production runs: entities must match the
            // schema that Flyway just created
            "spring.jpa.hibernate.ddl-auto=validate"
        })
class MySqlMigrationIntegrationTest {

    @Container @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TagRepository tagRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void flywayAppliedBothMigrations() {
        Integer applied =
                jdbc.queryForObject(
                        "select count(*) from flyway_schema_history where success = 1",
                        Integer.class);
        assertThat(applied).isEqualTo(2);
    }

    @Test
    void referenceDataIsSeeded() {
        assertThat(tagRepository.count()).as("seeded tags").isGreaterThan(0);
        assertThat(genreRepository.count()).as("seeded genres").isGreaterThan(0);
    }

    @Test
    void entitiesRoundTripAgainstTheRealSchema() {
        User saved = userRepository.save(new User("mysql-it-user", "irrelevant"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByNameIgnoreCase("mysql-it-user")).isPresent();
    }
}
