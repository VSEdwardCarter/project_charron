package com.charron.api.testing;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

public class TestDatabaseGuard
        implements ApplicationContextInitializer<
        ConfigurableApplicationContext> {

    private static final String EXPECTED_URL =
            "jdbc:postgresql://127.0.0.1:5435/charron_test";

    private static final String EXPECTED_USER =
            "charron_test";

    private static final String EXPECTED_VPIC_URL =
            "jdbc:postgresql://127.0.0.1:5436/vpic_test";

    private static final String EXPECTED_VPIC_USER =
            "vpic_test_reader";

    @Override
    public void initialize(
            ConfigurableApplicationContext context
    ) {

        Environment environment = context.getEnvironment();

        // Operational database protection.
        require(
                environment,
                "spring.datasource.url",
                EXPECTED_URL
        );

        require(
                environment,
                "spring.datasource.username",
                EXPECTED_USER
        );

        // Flyway database protection.
        require(
                environment,
                "spring.flyway.url",
                EXPECTED_URL
        );

        require(
                environment,
                "spring.flyway.user",
                EXPECTED_USER
        );

        // Isolated vPIC reference database protection.
        require(
                environment,
                "charron.vpic.enabled",
                "true"
        );

        require(
                environment,
                "VPIC_JDBC_URL",
                EXPECTED_VPIC_URL
        );

        require(
                environment,
                "VPIC_DB_USERNAME",
                EXPECTED_VPIC_USER
        );
    }

    private void require(
            Environment environment,
            String property,
            String expected
    ) {

        String actual = environment.getProperty(property);

        if (!expected.equals(actual)) {

            throw new IllegalStateException(
                    "TEST DATABASE SAFETY VIOLATION: "
                            + property
                            + " does not match the permitted "
                            + "test configuration. "
                            + "Application startup aborted."
            );
        }
    }
}