package com.tollfinder.spatial;

import com.tollfinder.domain.DrivingRoute;
import com.tollfinder.domain.GeoPoint;
import java.nio.file.Path;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import static org.assertj.core.api.Assertions.*;

// Opt-in only: the supplied database must be a disposable, explicitly approved test database.
@EnabledIfEnvironmentVariable(named = "TOLL_FINDER_RUN_DB_TESTS", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostgisRepositoryIT {
    private Connection connection;
    private String schema;
    private JdbcClient jdbc;
    private PostgisTollPlazaRepository repository;

    @BeforeAll
    void setUp() throws Exception {
        connection = DriverManager.getConnection(System.getenv("TEST_DB_URL"),
                System.getenv("TEST_DB_USERNAME"), System.getenv("TEST_DB_PASSWORD"));
        schema = "toll_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var statement = connection.createStatement()) {
            // PostGIS must already be installed in public by the test database owner.
            statement.execute("SELECT public.postgis_version()");
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema + ", public");
        }
        try (var statement = connection.createStatement()) {
            statement.execute(Files.readString(Path.of("docs/database/0001-toll-plaza-schema.sql")));
        }
        jdbc = JdbcClient.create(new SingleConnectionDataSource(connection, true));
        repository = new PostgisTollPlazaRepository(jdbc);
    }

    @BeforeEach
    void clearTestData() {
        jdbc.sql("DELETE FROM toll_plaza").update();
    }

    @AfterAll
    void cleanUp() throws Exception {
        if (connection != null) {
            try (var statement = connection.createStatement()) {
                statement.execute("ROLLBACK");
                if (schema != null) statement.execute("DROP SCHEMA " + schema + " CASCADE");
            } finally {
                connection.close();
            }
        }
    }

    @Test
    void matchesCorridorOrdersAlongCurvedRouteAndReversesDistances() {
        insert("First", 77.005, 28.00005);
        insert("Second", 77.01, 28.005);
        insert("Far away", 77.005, 28.01);
        var route = new DrivingRoute(List.of(new GeoPoint(28, 77), new GeoPoint(28, 77.01),
                new GeoPoint(28.01, 77.01)), 2100);
        var ids = repository.findCandidates(new GridCell(77, 28)).stream().map(p -> p.id()).toList();
        var matches = repository.findMatches(route, ids, 50);
        assertThat(matches).extracting(p -> p.name()).containsExactly("First", "Second");
        assertThat(matches.getFirst().distanceFromSourceMeters()).isBetween(480.0, 510.0);
        assertThat(matches.getLast().distanceFromSourceMeters()).isBetween(1500.0, 1580.0);
        var reverse = new DrivingRoute(route.points().reversed(), 2100);
        assertThat(repository.findMatches(reverse, ids, 50)).extracting(p -> p.name())
                .containsExactly("Second", "First");
    }

    @Test
    void usesMetersAtTheBoundaryAndInvalidatesDatasetVersion() {
        long before = repository.datasetVersion();
        jdbc.sql("""
                INSERT INTO toll_plaza(name, state, location)
                SELECT label, 'Test', ST_Project(ST_SetSRID(ST_Point(77.005, 28),4326)::geography,
                    meters, 0)::geometry
                FROM (VALUES ('Inside', 49.0), ('Outside', 51.0)) AS sample(label, meters)
                """).update();
        assertThat(repository.datasetVersion()).isGreaterThan(before);
        var route = new DrivingRoute(List.of(new GeoPoint(28, 77), new GeoPoint(28, 77.01)), 1000);
        var ids = repository.findCandidates(new GridCell(77, 28)).stream().map(p -> p.id()).toList();
        assertThat(repository.findMatches(route, ids, 50)).extracting(p -> p.name()).containsExactly("Inside");
        assertThat(repository.findMatches(route, List.of(), 50)).isEmpty();
    }

    @Test
    void preventsExactDuplicates() {
        insert("Plaza", 77.005, 28.005);
        assertThatThrownBy(() -> insert("Plaza", 77.005, 28.005))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    private void insert(String name, double longitude, double latitude) {
        jdbc.sql("INSERT INTO toll_plaza(name,state,location) VALUES (:name,'Test',ST_SetSRID(ST_Point(:lon,:lat),4326))")
                .param("name", name).param("lon", longitude).param("lat", latitude).update();
    }
}
