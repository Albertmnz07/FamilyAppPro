package albertmnz07.demo.family;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * The schema is owned by versioned migrations, not by Hibernate: every migration file in the
 * repository is applied on startup, and Hibernate only checks the migrated schema against the
 * entities.
 */
@SpringBootTest
class SchemaMigrationTest {

    @Autowired JdbcTemplate jdbc;

    @Autowired ConfigurableEnvironment environment;

    @Test
    void everyMigrationInTheRepositoryHasBeenApplied() throws Exception {
        assertThat(appliedMigrations())
                .containsExactlyInAnyOrderElementsOf(migrationsInTheRepository());
    }

    @Test
    void theMigratedSchemaIsTheOneTheEntitiesExpect() throws SQLException {
        assertThat(columnsOf("persona"))
                .contains(
                        "id",
                        "username",
                        "dni",
                        "password_hash",
                        "front_photo_key",
                        "front_photo_content_type",
                        "back_photo_key",
                        "back_photo_content_type");
        assertThat(columnsOf("family")).contains("id", "name", "admin_id");
        assertThat(columnsOf("membership")).contains("id", "family_id", "persona_id", "status");
        assertThat(columnsOf("consent"))
                .contains("id", "family_id", "persona_id", "granted_at", "revoked_at");
        assertThat(columnsOf("invite"))
                .contains("id", "code", "family_id", "expires_at", "redeemed_at");
        assertThat(columnsOf("auth_token"))
                .contains("id", "token", "persona_id", "expires_at", "created_at");
    }

    @Test
    void theLegacyPhotoColumnsAreGone() throws SQLException {
        assertThat(columnsOf("persona")).doesNotContain("front_photo", "back_photo");
    }

    @Test
    void dniPhotoColumnsAreOptionalButTheDniIsStillRequired() throws SQLException {
        assertThat(nullableOf("persona", "dni")).isEqualTo("NO");
        assertThat(nullableOf("persona", "front_photo_key")).isEqualTo("YES");
        assertThat(nullableOf("persona", "front_photo_content_type")).isEqualTo("YES");
        assertThat(nullableOf("persona", "back_photo_key")).isEqualTo("YES");
        assertThat(nullableOf("persona", "back_photo_content_type")).isEqualTo("YES");
    }

    @Test
    void theApplicationNeverChangesTheSchemaByItself() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(declaredDdlAutoValues()).doesNotContain("update");
    }

    @Test
    void aLegacySchemaIsBaselinedAtV1AndReceivesLaterMigrations() throws SQLException {
        DriverManagerDataSource legacy =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:tn09_legacy_upgrade;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        try (Connection connection = legacy.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "create table persona (id uuid primary key, username varchar(30) not null, "
                            + "dni varchar(255) not null, front_photo bytea not null, back_photo bytea not null, "
                            + "front_photo_content_type varchar(255) not null, back_photo_content_type varchar(255) not null)");
            statement.execute(
                    "insert into persona (id, username, dni, front_photo, back_photo, "
                            + "front_photo_content_type, back_photo_content_type) values "
                            + "('6e4b3b2a-2f6b-4b7e-9c2c-8a2f1a6d5b11', 'legacy', '12345678Z', "
                            + "X'00', X'00', 'image/jpeg', 'image/jpeg')");
        }

        // Mirrors the production baseline settings: skip V1 on a schema that predates Flyway.
        Flyway.configure()
                .dataSource(legacy)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load()
                .migrate();

        JdbcTemplate legacyJdbc = new JdbcTemplate(legacy);
        assertThat(
                        legacyJdbc.queryForList(
                                "select type || '|' || version from flyway_schema_history "
                                        + "where success = true order by installed_rank",
                                String.class))
                .contains("BASELINE|1", "SQL|2", "SQL|3");
        assertThat(columnsOf(legacyJdbc, "persona")).doesNotContain("front_photo", "back_photo");
        assertThat(nullableOf(legacyJdbc, "persona", "front_photo_content_type")).isEqualTo("YES");
        assertThat(nullableOf(legacyJdbc, "persona", "back_photo_content_type")).isEqualTo("YES");
        assertThat(
                        legacyJdbc.queryForObject(
                                "select dni from persona where username = 'legacy'", String.class))
                .isEqualTo("12345678Z");
    }

    private List<String> appliedMigrations() {
        return jdbc.query(
                "select version, description from flyway_schema_history "
                        + "where success = true and version is not null order by installed_rank",
                (rs, rowNum) -> rs.getString("version") + "|" + rs.getString("description"));
    }

    private List<String> migrationsInTheRepository() throws Exception {
        Resource[] migrations =
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:db/migration/V*__*.sql");
        return Arrays.stream(migrations)
                .map(Resource::getFilename)
                .map(SchemaMigrationTest::describe)
                .toList();
    }

    private static String describe(String fileName) {
        String withoutExtension = fileName.substring(0, fileName.length() - ".sql".length());
        int separator = withoutExtension.indexOf("__");
        String version = withoutExtension.substring(1, separator);
        String description = withoutExtension.substring(separator + 2).replace('_', ' ');
        return version + "|" + description;
    }

    private List<String> columnsOf(String table) throws SQLException {
        return columnsOf(jdbc, table);
    }

    private String nullableOf(String table, String column) throws SQLException {
        return nullableOf(jdbc, table, column);
    }

    private String nullableOf(JdbcTemplate jdbcTemplate, String table, String column)
            throws SQLException {
        return jdbcTemplate.execute(
                (ConnectionCallback<String>)
                        connection -> {
                            try (ResultSet resultSet =
                                    connection
                                            .getMetaData()
                                            .getColumns(null, null, table, column)) {
                                resultSet.next();
                                return resultSet.getString("IS_NULLABLE");
                            }
                        });
    }

    private List<String> columnsOf(JdbcTemplate jdbcTemplate, String table) throws SQLException {
        return jdbcTemplate.execute(
                (ConnectionCallback<List<String>>)
                        connection -> {
                            List<String> columns = new ArrayList<>();
                            try (ResultSet resultSet =
                                    connection.getMetaData().getColumns(null, null, table, "%")) {
                                while (resultSet.next()) {
                                    columns.add(resultSet.getString("COLUMN_NAME"));
                                }
                            }
                            return columns;
                        });
    }

    private List<String> declaredDdlAutoValues() {
        return environment.getPropertySources().stream()
                .filter(EnumerablePropertySource.class::isInstance)
                .map(EnumerablePropertySource.class::cast)
                .map(source -> source.getProperty("spring.jpa.hibernate.ddl-auto"))
                .filter(value -> value != null)
                .map(Object::toString)
                .toList();
    }
}
