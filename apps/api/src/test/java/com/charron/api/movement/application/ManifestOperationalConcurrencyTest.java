package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestChangeType;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.MovementEquipmentRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ManifestOperationalConcurrencyTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    @Autowired
    private MovementEquipmentAssignmentService assignmentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Movement movement;
    private UUID equipmentId;

    private final UUID reviewerId = UUID.randomUUID();
    private final UUID issuerId = UUID.randomUUID();

    private enum Outcome {
        ISSUED,
        STALE
    }

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-MANIFEST-OPERATIONAL-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        equipmentId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id, name, category
                )
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "Manifest Concurrency Test Trailer",
                "TRAILER"
        );
    }

    private TransactionTemplate transaction() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED
        );

        transaction.setTimeout(30);

        return transaction;
    }

    private MovementManifest generateReviewedManifest() {

        MovementManifest manifest =
                manifestService.generateAndPersist(
                        movement.id()
                );

        lifecycleService.review(
                manifest.id(),
                reviewerId,
                "Operational concurrency review"
        );

        return manifest;
    }

    private void assignEquipment() {

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
        );
    }

    private ManifestStatus status(UUID manifestId) {

        return lifecycleService
                .getLifecycle(manifestId)
                .status();
    }

    private Outcome attemptIssuance(UUID manifestId) {

        try {

            lifecycleService.issue(
                    manifestId,
                    issuerId,
                    "Operational concurrency issuance"
            );

            return Outcome.ISSUED;

        } catch (ManifestFreshnessConflictException exception) {

            assertEquals(
                    ManifestFreshnessStatus.STALE,
                    exception.getAssessment().status()
            );

            assertTrue(
                    exception.getAssessment()
                            .changes()
                            .contains(ManifestChangeType.EQUIPMENT)
            );

            return Outcome.STALE;
        }
    }

    private void await(CountDownLatch latch) {

        try {

            assertTrue(
                    latch.await(10, TimeUnit.SECONDS),
                    "Timed out waiting for concurrent operation"
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Concurrent operation interrupted",
                    exception
            );
        }
    }

    private void awaitDatabaseLock(int backendPid)
            throws InterruptedException {

        long deadline = System.nanoTime()
                + TimeUnit.SECONDS.toNanos(10);

        while (System.nanoTime() < deadline) {

            Boolean blocked = jdbcTemplate.queryForObject(
                    """
                    SELECT cardinality(
                        pg_blocking_pids(?)
                    ) > 0
                    """,
                    Boolean.class,
                    backendPid
            );

            if (Boolean.TRUE.equals(blocked)) {
                return;
            }

            Thread.sleep(25);
        }

        fail("Expected PostgreSQL lock contention was not observed");
    }

    private Outcome modifyBeforeIssuance(
            UUID manifestId,
            boolean rollback
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch modificationReady =
                new CountDownLatch(1);

        CountDownLatch releaseModification =
                new CountDownLatch(1);

        CompletableFuture<Integer> issuanceBackend =
                new CompletableFuture<>();

        try {

            Future<?> modification = executor.submit(
                    () -> transaction()
                            .executeWithoutResult(status -> {

                                assignEquipment();

                                modificationReady.countDown();

                                await(releaseModification);

                                if (rollback) {
                                    status.setRollbackOnly();
                                }
                            })
            );

            await(modificationReady);

            Future<Outcome> issuance = executor.submit(
                    () -> {

                        try {

                            return transaction().execute(status -> {

                                issuanceBackend.complete(
                                        jdbcTemplate.queryForObject(
                                                "SELECT pg_backend_pid()",
                                                Integer.class
                                        )
                                );

                                return attemptIssuance(manifestId);
                            });

                        } catch (
                                org.springframework.transaction
                                        .UnexpectedRollbackException exception
                        ) {

                            // A rejected issuance marks the joined
                            // transaction rollback-only.

                            return Outcome.STALE;
                        }
                    }
            );

            awaitDatabaseLock(
                    issuanceBackend.get(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            assertFalse(issuance.isDone());

            releaseModification.countDown();

            modification.get(30, TimeUnit.SECONDS);

            return issuance.get(30, TimeUnit.SECONDS);

        } finally {

            releaseModification.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            30,
                            TimeUnit.SECONDS
                    )
            );
        }
    }

    @Test
    void rejectsIssuanceAfterConcurrentModificationCommits()
            throws Exception {

        MovementManifest manifest =
                generateReviewedManifest();

        Outcome outcome = modifyBeforeIssuance(
                manifest.id(),
                false
        );

        assertEquals(Outcome.STALE, outcome);

        assertEquals(
                ManifestStatus.REVIEWED,
                status(manifest.id())
        );

        assertEquals(
                1,
                eventRepository.findByManifestId(
                        manifest.id()
                ).size()
        );

        assertEquals(
                1,
                assignmentService.findByMovementId(
                        movement.id()
                ).size()
        );

        assertEquals(
                manifest,
                manifestRepository.findById(
                        manifest.id()
                ).orElseThrow()
        );

        var assessment =
                manifestService.assessFreshness(manifest.id());

        assertEquals(
                ManifestFreshnessStatus.STALE,
                assessment.status()
        );

        assertTrue(
                assessment.changes()
                        .contains(ManifestChangeType.EQUIPMENT)
        );
    }

    @Test
    void permitsIssuanceAfterConcurrentModificationRollsBack()
            throws Exception {

        MovementManifest manifest =
                generateReviewedManifest();

        Outcome outcome = modifyBeforeIssuance(
                manifest.id(),
                true
        );

        assertEquals(Outcome.ISSUED, outcome);

        assertEquals(
                ManifestStatus.ISSUED,
                status(manifest.id())
        );

        assertTrue(
                assignmentService.findByMovementId(
                        movement.id()
                ).isEmpty()
        );

        assertEquals(
                2,
                eventRepository.findByManifestId(
                        manifest.id()
                ).size()
        );

        assertTrue(
                manifestService.assessFreshness(
                        manifest.id()
                ).isCurrent()
        );
    }

    @Test
    void preservesIssuedSnapshotWhenModificationCommitsAfterIssuance()
            throws Exception {

        MovementManifest manifest =
                generateReviewedManifest();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch issuanceReady =
                new CountDownLatch(1);

        CountDownLatch releaseIssuance =
                new CountDownLatch(1);

        CompletableFuture<Integer> modificationBackend =
                new CompletableFuture<>();

        try {

            Future<?> issuance = executor.submit(
                    () -> transaction()
                            .executeWithoutResult(status -> {

                                lifecycleService.issue(
                                        manifest.id(),
                                        issuerId,
                                        "Issue before modification"
                                );

                                issuanceReady.countDown();

                                await(releaseIssuance);
                            })
            );

            await(issuanceReady);

            Future<?> modification = executor.submit(
                    () -> transaction()
                            .executeWithoutResult(status -> {

                                modificationBackend.complete(
                                        jdbcTemplate.queryForObject(
                                                "SELECT pg_backend_pid()",
                                                Integer.class
                                        )
                                );

                                assignEquipment();
                            })
            );

            awaitDatabaseLock(
                    modificationBackend.get(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            assertFalse(modification.isDone());

            releaseIssuance.countDown();

            issuance.get(30, TimeUnit.SECONDS);
            modification.get(30, TimeUnit.SECONDS);

            assertEquals(
                    ManifestStatus.ISSUED,
                    status(manifest.id())
            );

            assertEquals(
                    manifest,
                    manifestRepository.findById(
                            manifest.id()
                    ).orElseThrow()
            );

            assertEquals(
                    2,
                    eventRepository.findByManifestId(
                            manifest.id()
                    ).size()
            );

            assertEquals(
                    1,
                    assignmentService.findByMovementId(
                            movement.id()
                    ).size()
            );

            assertTrue(
                    manifestService.assessFreshness(
                            manifest.id()
                    ).isStale()
            );

        } finally {

            releaseIssuance.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            30,
                            TimeUnit.SECONDS
                    )
            );
        }
    }
}