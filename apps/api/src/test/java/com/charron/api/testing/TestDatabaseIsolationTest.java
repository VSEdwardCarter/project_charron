package com.charron.api.testing;

import org.flywaydb.core.Flyway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TestDatabaseIsolationTest {

    @Autowired
    @Qualifier("vpicDataSource")
    private DataSource vpicDataSource;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void applicationUsesIsolatedDatabase() throws SQLException {

        assertIsolatedDatabase(dataSource);
    }

    @Test
    void flywayUsesIsolatedDatabase() throws SQLException {

        assertIsolatedDatabase(
                flyway.getConfiguration().getDataSource()
        );
    }

    @Test
    void flywayAppliedRequiredMigrations() {

        assertNotNull(flyway.info().current());

        assertEquals(
                "26",
                flyway.info()
                        .current()
                        .getVersion()
                        .getVersion()
        );

        assertEquals(
                0,
                flyway.info().pending().length
        );
    }

    private void assertIsolatedDatabase(
            DataSource source
    ) throws SQLException {

        try (Connection connection = source.getConnection()) {

            assertEquals(
                    "jdbc:postgresql://127.0.0.1:5435/charron_test",
                    connection.getMetaData().getURL()
            );

            try (
                    var statement = connection.createStatement();

                    var result = statement.executeQuery("""
                            SELECT current_database(),
                                   current_user
                            """)
            ) {

                assertTrue(result.next());

                assertEquals(
                        "charron_test",
                        result.getString(1)
                );

                assertEquals(
                        "charron_test",
                        result.getString(2)
                );
            }
        }
    }

    @Test
    void vpicUsesIsolatedReferenceDatabase() throws SQLException {

        try (Connection connection =
                     vpicDataSource.getConnection()) {

            assertEquals(
                    "jdbc:postgresql://127.0.0.1:5436/vpic_test",
                    connection.getMetaData().getURL()
            );

            try (
                    var statement = connection.createStatement();

                    var result = statement.executeQuery("""
                        SELECT current_database(),
                               current_user
                        """)
            ) {

                assertTrue(result.next());

                assertEquals(
                        "vpic_test",
                        result.getString(1)
                );

                assertEquals(
                        "vpic_test_reader",
                        result.getString(2)
                );
            }
        }
    }

    @Test
    void vpicReaderCannotModifyPersistentReferenceTables() {

        JdbcTemplate vpic = new JdbcTemplate(vpicDataSource);

        Long writableTables = vpic.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_class
                WHERE relnamespace = 'vpic'::regnamespace
                  AND relkind IN ('r', 'p')
                  AND has_table_privilege(
                      current_user,
                      oid,
                      'INSERT,UPDATE,DELETE,TRUNCATE'
                  )
                """,
                Long.class
        );

        assertEquals(0L, writableTables);
    }

    @Test
    void isolatedVpicDecoderRemainsFunctional() {

        JdbcTemplate vpic = new JdbcTemplate(vpicDataSource);

        Long decodedRows = vpic.queryForObject(
                """
                SELECT COUNT(*)
                FROM vpic.spVinDecode(?)
                """,
                Long.class,
                "1HGCM82633A004352"
        );

        assertEquals(44L, decodedRows);
    }
}