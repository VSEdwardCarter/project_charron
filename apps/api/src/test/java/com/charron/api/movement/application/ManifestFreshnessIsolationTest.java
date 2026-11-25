package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ManifestFreshnessIsolationTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MovementManifest createManifest() {

        Movement movement = Movement.plan(
                "TEST-FRESHNESS-ISOLATION-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        return manifestService.generateAndPersist(
                movement.id()
        );
    }

    private TransactionTemplate createTransaction(
            int isolation
    ) {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setIsolationLevel(isolation);

        transaction.setReadOnly(true);

        return transaction;
    }

    private String databaseIsolation() {

        return jdbcTemplate.queryForObject(
                "SHOW transaction_isolation",
                String.class
        );
    }

    private String databaseReadOnly() {

        return jdbcTemplate.queryForObject(
                "SHOW transaction_read_only",
                String.class
        );
    }

    @Test
    void postgresSupportsRepeatableReadTransactions() {

        TransactionTemplate transaction =
                createTransaction(
                        TransactionDefinition.ISOLATION_REPEATABLE_READ
                );

        transaction.executeWithoutResult(status -> {

            assertTrue(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            assertEquals(
                    "repeatable read",
                    databaseIsolation()
            );

            assertEquals(
                    TransactionDefinition.ISOLATION_REPEATABLE_READ,
                    TransactionSynchronizationManager
                            .getCurrentTransactionIsolationLevel()
            );
        });
    }

    @Test
    void postgresSupportsReadOnlyRepeatableReadTransactions() {

        TransactionTemplate transaction =
                createTransaction(
                        TransactionDefinition.ISOLATION_REPEATABLE_READ
                );

        transaction.executeWithoutResult(status -> {

            assertEquals(
                    "repeatable read",
                    databaseIsolation()
            );

            assertEquals(
                    "on",
                    databaseReadOnly()
            );

            assertTrue(
                    TransactionSynchronizationManager
                            .isCurrentTransactionReadOnly()
            );
        });
    }

    @Test
    void freshnessAssessmentWorksWithoutOuterTransaction() {

        MovementManifest manifest = createManifest();

        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );

        var assessment =
                manifestService.assessFreshness(
                        manifest.id()
                );

        assertEquals(
                ManifestFreshnessStatus.CURRENT,
                assessment.status()
        );

        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );
    }

    @Test
    void freshnessAssessmentParticipatesInRepeatableReadTransaction() {

        MovementManifest manifest = createManifest();

        TransactionTemplate transaction =
                createTransaction(
                        TransactionDefinition.ISOLATION_REPEATABLE_READ
                );

        transaction.executeWithoutResult(status -> {

            assertEquals(
                    "repeatable read",
                    databaseIsolation()
            );

            assertTrue(
                    manifestService.assessFreshness(
                            manifest.id()
                    ).isCurrent()
            );

            assertEquals(
                    "repeatable read",
                    databaseIsolation()
            );
        });
    }

    @Test
    void existingReadCommittedTransactionIsNotAutomaticallyUpgraded() {

        MovementManifest manifest = createManifest();

        TransactionTemplate transaction =
                createTransaction(
                        TransactionDefinition.ISOLATION_READ_COMMITTED
                );

        transaction.executeWithoutResult(status -> {

            assertEquals(
                    "read committed",
                    databaseIsolation()
            );

            assertTrue(
                    manifestService.assessFreshness(
                            manifest.id()
                    ).isCurrent()
            );

            assertEquals(
                    "read committed",
                    databaseIsolation()
            );
        });
    }

    @Test
    void freshnessAssessmentDoesNotLeakTransactionContext() {

        MovementManifest manifest = createManifest();

        for (int attempt = 0; attempt < 3; attempt++) {

            assertTrue(
                    manifestService.assessFreshness(
                            manifest.id()
                    ).isCurrent()
            );

            assertFalse(
                    TransactionSynchronizationManager
                            .isActualTransactionActive()
            );

            assertNull(
                    TransactionSynchronizationManager
                            .getCurrentTransactionIsolationLevel()
            );

            assertFalse(
                    TransactionSynchronizationManager
                            .isCurrentTransactionReadOnly()
            );
        }
    }
}