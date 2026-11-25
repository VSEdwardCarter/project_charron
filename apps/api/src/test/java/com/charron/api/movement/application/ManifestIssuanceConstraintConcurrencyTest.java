package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ManifestIssuanceConstraintConcurrencyTest {

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

    private final UUID reviewerId = UUID.randomUUID();

    private enum Outcome {
        ISSUED,
        CONSTRAINT_VIOLATION
    }

    private Movement createMovement() {

        Movement movement = Movement.plan(
                "TEST-DB-ISSUE-CONCURRENT-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        return movement;
    }

    private MovementManifest generateReviewedManifest(
            UUID movementId
    ) {

        MovementManifest manifest =
                manifestService.generateAndPersist(movementId);

        lifecycleService.review(
                manifest.id(),
                reviewerId,
                "Database concurrency test review"
        );

        return manifest;
    }

    private Outcome attemptDirectIssuance(
            UUID manifestId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {

            throw new IllegalStateException(
                    "Timed out waiting for issuance start"
            );
        }

        ManifestLifecycleEvent event =
                new ManifestLifecycleEvent(
                        UUID.randomUUID(),
                        manifestId,
                        ManifestStatus.REVIEWED,
                        ManifestStatus.ISSUED,
                        UUID.randomUUID(),
                        Instant.now().truncatedTo(
                                ChronoUnit.MICROS
                        ),
                        "Direct concurrent database issuance"
                );

        try {

            eventRepository.save(event);

            return Outcome.ISSUED;

        } catch (DataIntegrityViolationException exception) {

            String message = exception
                    .getMostSpecificCause()
                    .getMessage();

            assertTrue(
                    message.contains(
                            "Another issued manifest already exists"
                    )
                            || message.contains(
                            "Cannot issue an older manifest version"
                    )
                            || message.contains(
                            "Invalid manifest lifecycle sequence"
                    ),
                    "Unexpected constraint violation: " + message
            );

            return Outcome.CONSTRAINT_VIOLATION;
        }
    }

    private List<Outcome> issueConcurrently(
            UUID firstManifestId,
            UUID secondManifestId
    ) throws Exception {

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {

            Future<Outcome> first = executor.submit(
                    () -> attemptDirectIssuance(
                            firstManifestId,
                            ready,
                            start
                    )
            );

            Future<Outcome> second = executor.submit(
                    () -> attemptDirectIssuance(
                            secondManifestId,
                            ready,
                            start
                    )
            );

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Issuance requests did not become ready"
            );

            start.countDown();

            return List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS)
            );

        } finally {

            start.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            30,
                            TimeUnit.SECONDS
                    ),
                    "Concurrent issuance requests did not terminate"
            );
        }
    }

    private long countIssuedManifests(UUID movementId) {

        return manifestRepository
                .findByMovementId(movementId)
                .stream()
                .map(manifest ->
                        lifecycleService.getLifecycle(manifest.id())
                )
                .filter(ManifestLifecycle::isIssued)
                .count();
    }

    private void assertSingleSuccessfulIssuance(
            List<Outcome> outcomes
    ) {

        assertEquals(
                1L,
                outcomes.stream()
                        .filter(outcome ->
                                outcome == Outcome.ISSUED)
                        .count()
        );

        assertEquals(
                1L,
                outcomes.stream()
                        .filter(outcome ->
                                outcome == Outcome.CONSTRAINT_VIOLATION)
                        .count()
        );
    }

    @Test
    void databaseRejectsConcurrentDuplicateIssuance()
            throws Exception {

        Movement movement = createMovement();

        MovementManifest manifest =
                generateReviewedManifest(movement.id());

        List<Outcome> outcomes = issueConcurrently(
                manifest.id(),
                manifest.id()
        );

        assertSingleSuccessfulIssuance(outcomes);

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService
                        .getLifecycle(manifest.id())
                        .status()
        );

        assertEquals(
                2,
                eventRepository
                        .findByManifestId(manifest.id())
                        .size()
        );

        assertEquals(
                1L,
                countIssuedManifests(movement.id())
        );
    }

    @Test
    void databaseRejectsConcurrentIssuanceOfCompetingVersions()
            throws Exception {

        Movement movement = createMovement();

        MovementManifest older =
                generateReviewedManifest(movement.id());

        MovementManifest newer =
                generateReviewedManifest(movement.id());

        List<Outcome> outcomes = issueConcurrently(
                older.id(),
                newer.id()
        );

        assertSingleSuccessfulIssuance(outcomes);

        MovementManifest winner =
                outcomes.getFirst() == Outcome.ISSUED
                        ? older
                        : newer;

        MovementManifest rejected =
                winner.id().equals(older.id())
                        ? newer
                        : older;

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService
                        .getLifecycle(winner.id())
                        .status()
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycleService
                        .getLifecycle(rejected.id())
                        .status()
        );

        assertEquals(
                2,
                eventRepository
                        .findByManifestId(winner.id())
                        .size()
        );

        assertEquals(
                1,
                eventRepository
                        .findByManifestId(rejected.id())
                        .size()
        );

        assertEquals(
                1L,
                countIssuedManifests(movement.id())
        );
    }

    @Test
    void databasePermitsConcurrentIssuanceForIndependentMovements()
            throws Exception {

        Movement firstMovement = createMovement();

        Movement secondMovement = createMovement();

        MovementManifest first =
                generateReviewedManifest(firstMovement.id());

        MovementManifest second =
                generateReviewedManifest(secondMovement.id());

        List<Outcome> outcomes = issueConcurrently(
                first.id(),
                second.id()
        );

        assertEquals(
                List.of(Outcome.ISSUED, Outcome.ISSUED),
                outcomes
        );

        assertEquals(
                1L,
                countIssuedManifests(firstMovement.id())
        );

        assertEquals(
                1L,
                countIssuedManifests(secondMovement.id())
        );
    }
}