package com.charron.api.movement.application;

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
class MovementQualityVerificationConcurrencyTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private MovementQualityVerificationService verificationService;

    @Autowired
    private MovementQualityVerificationRepository verificationRepository;

    @Autowired
    private MovementEquipmentAssignmentService assignmentService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Movement movement;

    private final UUID reviewerId = UUID.randomUUID();
    private final UUID issuerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        assertEquals(
                "charron_test",
                jdbcTemplate.queryForObject(
                        "SELECT current_database()",
                        String.class
                )
        );

        movement = Movement.plan(
                "TEST-QAQC-CONCURRENT-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);
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

    private MovementManifest generateIssuedManifest() {

        MovementManifest manifest =
                manifestService.generateAndPersist(
                        movement.id()
                );

        lifecycleService.review(
                manifest.id(),
                reviewerId,
                "QA/QC concurrency review"
        );

        lifecycleService.issue(
                manifest.id(),
                issuerId,
                "QA/QC concurrency issuance"
        );

        return manifest;
    }

    private void completeVerification(
            MovementManifest manifest
    ) {

        verificationService.complete(
                movement.id(),
                manifest.id(),
                reviewerId,
                true, true, true,
                true, true, true,
                "Concurrent verification test"
        );
    }

    private boolean isVerificationCurrent(
            MovementManifest manifest
    ) {

        return verificationRepository
                .existsForCurrentOperationalRevision(
                        manifest.id()
                );
    }

    private long currentOperationalRevision() {

        return jdbcTemplate.queryForObject(
                """
                SELECT operational_revision
                FROM movements
                WHERE id = ?
                """,
                Long.class,
                movement.id()
        );
    }

    private void changeDestination(String destination) {

        assertEquals(
                1,
                jdbcTemplate.update(
                        """
                        UPDATE movements
                        SET destination = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                        destination,
                        movement.id()
                )
        );
    }

    private UUID registerAssignedEquipment() {

        UUID equipmentId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id, name, category
                )
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "QA/QC Concurrency Trailer",
                "TRAILER"
        );

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
        );

        return equipmentId;
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

        fail(
                "Expected PostgreSQL lock contention "
                        + "was not observed"
        );
    }

    private void executeConcurrentOperations(
            Runnable firstOperation,
            Runnable secondOperation,
            boolean rollbackFirst,
            boolean rollbackSecond
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch firstReady =
                new CountDownLatch(1);

        CountDownLatch releaseFirst =
                new CountDownLatch(1);

        CompletableFuture<Integer> secondBackend =
                new CompletableFuture<>();

        try {

            Future<?> first = executor.submit(
                    () -> transaction()
                            .executeWithoutResult(status -> {

                                firstOperation.run();

                                firstReady.countDown();

                                await(releaseFirst);

                                if (rollbackFirst) {
                                    status.setRollbackOnly();
                                }
                            })
            );

            await(firstReady);

            Future<?> second = executor.submit(
                    () -> transaction()
                            .executeWithoutResult(status -> {

                                secondBackend.complete(
                                        jdbcTemplate.queryForObject(
                                                "SELECT pg_backend_pid()",
                                                Integer.class
                                        )
                                );

                                secondOperation.run();

                                if (rollbackSecond) {
                                    status.setRollbackOnly();
                                }
                            })
            );

            awaitDatabaseLock(
                    secondBackend.get(
                            10,
                            TimeUnit.SECONDS
                    )
            );

            assertFalse(second.isDone());

            releaseFirst.countDown();

            first.get(30, TimeUnit.SECONDS);

            second.get(30, TimeUnit.SECONDS);

        } finally {

            releaseFirst.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            30,
                            TimeUnit.SECONDS
                    )
            );
        }
    }

    // ========================================================
    // Concurrent operational modification before verification
    // ========================================================

    @Test
    void rejectsVerificationAfterConcurrentModificationCommits()
            throws Exception {

        MovementManifest manifest =
                generateIssuedManifest();

        ExecutionException exception = assertThrows(
                ExecutionException.class,
                () -> executeConcurrentOperations(
                        () -> changeDestination("Houston, TX"),
                        () -> completeVerification(manifest),
                        false,
                        false
                )
        );

        assertInstanceOf(
                ManifestFreshnessConflictException.class,
                exception.getCause()
        );

        assertFalse(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );

        assertFalse(isVerificationCurrent(manifest));

        assertTrue(
                manifestService.assessFreshness(
                        manifest.id()
                ).isStale()
        );
    }

    // ========================================================
    // Verification after rolled-back operational modification
    // ========================================================

    @Test
    void permitsVerificationAfterConcurrentModificationRollsBack()
            throws Exception {

        MovementManifest manifest =
                generateIssuedManifest();

        long originalRevision =
                currentOperationalRevision();

        executeConcurrentOperations(
                () -> changeDestination("Houston, TX"),
                () -> completeVerification(manifest),
                true,
                false
        );

        assertEquals(
                originalRevision,
                currentOperationalRevision()
        );

        assertTrue(isVerificationCurrent(manifest));

        assertTrue(
                manifestService.assessFreshness(
                        manifest.id()
                ).isCurrent()
        );
    }

    // ========================================================
    // Committed modification after verification
    // ========================================================

    @Test
    void committedModificationAfterVerificationInvalidatesEligibility()
            throws Exception {

        MovementManifest manifest =
                generateIssuedManifest();

        long originalRevision =
                currentOperationalRevision();

        executeConcurrentOperations(
                () -> completeVerification(manifest),
                () -> changeDestination("Houston, TX"),
                false,
                false
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );

        assertTrue(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );

        assertFalse(isVerificationCurrent(manifest));
    }

    // ========================================================
    // Rolled-back modification after verification
    // ========================================================

    @Test
    void rolledBackModificationAfterVerificationPreservesEligibility()
            throws Exception {

        MovementManifest manifest =
                generateIssuedManifest();

        long originalRevision =
                currentOperationalRevision();

        executeConcurrentOperations(
                () -> completeVerification(manifest),
                () -> changeDestination("Houston, TX"),
                false,
                true
        );

        assertEquals(
                originalRevision,
                currentOperationalRevision()
        );

        assertTrue(isVerificationCurrent(manifest));
    }

    // ========================================================
    // Concurrent operational revision increments
    // ========================================================

    @Test
    void concurrentOperationalChangesDoNotLoseRevisionIncrements()
            throws Exception {

        long originalRevision =
                currentOperationalRevision();

        executeConcurrentOperations(
                () -> changeDestination("Houston, TX"),
                () -> changeDestination("Austin, TX"),
                false,
                false
        );

        assertEquals(
                originalRevision + 2,
                currentOperationalRevision()
        );

        assertEquals(
                "Austin, TX",
                movementRepository.findById(
                        movement.id()
                ).orElseThrow().destination()
        );
    }

    // ========================================================
    // Concurrent shared equipment catalog modification
    // ========================================================

    @Test
    void concurrentCatalogChangeInvalidatesVerification()
            throws Exception {

        UUID equipmentId = registerAssignedEquipment();

        MovementManifest manifest =
                generateIssuedManifest();

        long originalRevision =
                currentOperationalRevision();

        executeConcurrentOperations(
                () -> completeVerification(manifest),
                () -> jdbcTemplate.update(
                        """
                        UPDATE equipment
                        SET name = ?
                        WHERE id = ?
                        """,
                        "Modified Concurrency Trailer",
                        equipmentId
                ),
                false,
                false
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );

        assertTrue(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );

        assertFalse(isVerificationCurrent(manifest));
    }
}