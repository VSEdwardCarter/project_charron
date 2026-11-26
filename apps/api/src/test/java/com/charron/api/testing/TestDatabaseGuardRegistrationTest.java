package com.charron.api.testing;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.*;

class TestDatabaseGuardRegistrationTest {

    @Configuration(proxyBeanMethods = false)
    static class GuardProbeConfiguration {
    }

    @Test
    void automaticallyRejectsUnsafeDatabaseConfiguration() {

        SpringApplication application =
                new SpringApplication(
                        GuardProbeConfiguration.class
                );

        application.setWebApplicationType(
                WebApplicationType.NONE
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> {
                    try (var context = application.run(
                            "--spring.datasource.url="
                                    + "jdbc:postgresql://127.0.0.1:1/unsafe_probe"
                    )) {
                        fail("Database guard did not prevent startup");
                    }
                }
        );

        assertAll(
                () -> assertTrue(
                        exception.getMessage().contains(
                                "TEST DATABASE SAFETY VIOLATION"
                        )
                ),
                () -> assertTrue(
                        exception.getMessage().contains(
                                "spring.datasource.url"
                        )
                )
        );
    }
}