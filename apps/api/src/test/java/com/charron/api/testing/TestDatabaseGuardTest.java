package com.charron.api.testing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class TestDatabaseGuardTest {

    private static final String TEST_URL =
            "jdbc:postgresql://127.0.0.1:5435/charron_test";

    private static final String TEST_VPIC_URL =
            "jdbc:postgresql://127.0.0.1:5436/vpic_test";

    private TestDatabaseGuard guard;

    private MockEnvironment environment;

    @BeforeEach
    void setUp() {

        guard = new TestDatabaseGuard();

        environment = new MockEnvironment()
                .withProperty(
                        "spring.datasource.url",
                        TEST_URL
                )
                .withProperty(
                        "spring.datasource.username",
                        "charron_test"
                )
                .withProperty(
                        "spring.flyway.url",
                        TEST_URL
                )
                .withProperty(
                        "spring.flyway.user",
                        "charron_test"
                )
                .withProperty(
                        "charron.vpic.enabled",
                        "true"
                )
                .withProperty(
                        "VPIC_JDBC_URL",
                        TEST_VPIC_URL
                )
                .withProperty(
                        "VPIC_DB_USERNAME",
                        "vpic_test_reader"
                );
    }

    @Test
    void permitsIsolatedTestDatabases() {

        assertDoesNotThrow(this::initializeGuard);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://localhost:5433/charron_dev",
            "jdbc:postgresql://localhost:5432/charron_beta",
            "jdbc:postgresql://localhost:5434/vpic",
            "jdbc:postgresql://127.0.0.1:5435/other_database"
    })
    void rejectsUnsafeDatasourceUrl(String unsafeUrl) {

        environment.setProperty(
                "spring.datasource.url",
                unsafeUrl
        );

        assertSafetyViolation("spring.datasource.url");
    }

    @Test
    void rejectsDevelopmentDatabaseUser() {

        environment.setProperty(
                "spring.datasource.username",
                "charron_dev"
        );

        assertSafetyViolation("spring.datasource.username");
    }

    @Test
    void rejectsUnsafeFlywayDatabase() {

        environment.setProperty(
                "spring.flyway.url",
                "jdbc:postgresql://localhost:5433/charron_dev"
        );

        assertSafetyViolation("spring.flyway.url");
    }

    @Test
    void rejectsUnsafeFlywayUser() {

        environment.setProperty(
                "spring.flyway.user",
                "charron_dev"
        );

        assertSafetyViolation("spring.flyway.user");
    }

    @Test
    void rejectsDisabledVpicConnectivity() {

        environment.setProperty(
                "charron.vpic.enabled",
                "false"
        );

        assertSafetyViolation("charron.vpic.enabled");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:postgresql://127.0.0.1:5434/vpic",
            "jdbc:postgresql://localhost:5434/vpic",
            "jdbc:postgresql://127.0.0.1:5433/charron_dev",
            "jdbc:postgresql://127.0.0.1:5435/charron_test",
            "jdbc:postgresql://127.0.0.1:5436/other_database"
    })
    void rejectsUnsafeVpicDatabase(String unsafeUrl) {

        environment.setProperty(
                "VPIC_JDBC_URL",
                unsafeUrl
        );

        assertSafetyViolation("VPIC_JDBC_URL");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "vpic",
            "postgres",
            "vpic_test_admin",
            "charron_test"
    })
    void rejectsUnsafeVpicUser(String unsafeUser) {

        environment.setProperty(
                "VPIC_DB_USERNAME",
                unsafeUser
        );

        assertSafetyViolation("VPIC_DB_USERNAME");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.flyway.url",
            "spring.flyway.user",
            "charron.vpic.enabled",
            "VPIC_JDBC_URL",
            "VPIC_DB_USERNAME"
    })
    void rejectsMissingRequiredConfiguration(String property) {

        environment.setProperty(property, "");

        assertSafetyViolation(property);
    }

    private void initializeGuard() {

        try (GenericApplicationContext context =
                     new GenericApplicationContext()) {

            context.setEnvironment(environment);

            guard.initialize(context);
        }
    }

    private void assertSafetyViolation(String property) {

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                this::initializeGuard
        );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "TEST DATABASE SAFETY VIOLATION"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains(property)
                )
        );
    }
}