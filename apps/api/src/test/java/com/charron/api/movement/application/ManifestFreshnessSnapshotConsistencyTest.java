package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestChangeType;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ManifestFreshnessSnapshotConsistencyTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private MovementManifest createManifest() {

        Movement movement = Movement.plan(
                "TEST-FRESHNESS-SNAPSHOT-" + UUID.randomUUID(),
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

    private TransactionTemplate readTransaction(
            int isolation
    ) {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setIsolationLevel(isolation);
        transaction.setReadOnly(true);
        transaction.setTimeout(30);

        return transaction;
    }

    private void updateDestinationIndependently(
            UUID movementId,
            String destination
    ) {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        transaction.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED
        );

        transaction.setTimeout(30);

        transaction.executeWithoutResult(status -> {

            int updated = jdbcTemplate.update(
                    """
                    UPDATE movements
                    SET destination = ?,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """,
                    destination,
                    movementId
            );

            assertEquals(1, updated);
        });
    }

    private ManifestFreshnessAssessment assess(
            MovementManifest manifest
    ) {

        return manifestService.assessFreshness(
                manifest.id()
        );
    }

    private void assertCurrent(
            MovementManifest manifest
    ) {

        ManifestFreshnessAssessment assessment =
                assess(manifest);

        assertEquals(
                ManifestFreshnessStatus.CURRENT,
                assessment.status()
        );

        assertTrue(
                assessment.changes().isEmpty()
        );
    }

    private void assertDestinationStale(
            MovementManifest manifest
    ) {

        ManifestFreshnessAssessment assessment =
                assess(manifest);

        assertEquals(
                ManifestFreshnessStatus.STALE,
                assessment.status()
        );

        assertEquals(
                Set.of(ManifestChangeType.DESTINATION),
                assessment.changes()
        );
    }

    @Test
    void repeatableReadPreservesOriginalSnapshot() {

        MovementManifest manifest = createManifest();

        TransactionTemplate reader =
                readTransaction(
                        TransactionDefinition
                                .ISOLATION_REPEATABLE_READ
                );

        reader.executeWithoutResult(status -> {

            assertCurrent(manifest);

            updateDestinationIndependently(
                    manifest.movementId(),
                    "Houston, TX"
            );

            assertCurrent(manifest);
            assertCurrent(manifest);

            Movement visibleMovement =
                    movementRepository.findById(
                            manifest.movementId()
                    ).orElseThrow();

            assertEquals(
                    "Dallas, TX",
                    visibleMovement.destination()
            );
        });

        assertDestinationStale(manifest);
    }

    @Test
    void readCommittedObservesNewlyCommittedChanges() {

        MovementManifest manifest = createManifest();

        TransactionTemplate reader =
                readTransaction(
                        TransactionDefinition
                                .ISOLATION_READ_COMMITTED
                );

        reader.executeWithoutResult(status -> {

            assertCurrent(manifest);

            updateDestinationIndependently(
                    manifest.movementId(),
                    "Houston, TX"
            );

            assertDestinationStale(manifest);

            Movement visibleMovement =
                    movementRepository.findById(
                            manifest.movementId()
                    ).orElseThrow();

            assertEquals(
                    "Houston, TX",
                    visibleMovement.destination()
            );
        });
    }

    @Test
    void independentAssessmentsObserveLatestCommittedState() {

        MovementManifest manifest = createManifest();

        assertCurrent(manifest);

        updateDestinationIndependently(
                manifest.movementId(),
                "Houston, TX"
        );

        assertDestinationStale(manifest);

        updateDestinationIndependently(
                manifest.movementId(),
                "Dallas, TX"
        );

        assertCurrent(manifest);
    }

    @Test
    void concurrentOperationalChangePreservesHistoricalSnapshot() {

        MovementManifest manifest = createManifest();

        TransactionTemplate reader =
                readTransaction(
                        TransactionDefinition
                                .ISOLATION_REPEATABLE_READ
                );

        reader.executeWithoutResult(status -> {

            assertCurrent(manifest);

            updateDestinationIndependently(
                    manifest.movementId(),
                    "Houston, TX"
            );

            assertCurrent(manifest);
        });

        assertDestinationStale(manifest);

        MovementManifest persisted =
                manifestRepository.findById(
                        manifest.id()
                ).orElseThrow();

        assertEquals(manifest, persisted);

        assertEquals(
                "Dallas, TX",
                persisted.destination()
        );

        assertEquals(
                1,
                manifestRepository.findLatestVersion(
                        manifest.movementId()
                )
        );
    }
}