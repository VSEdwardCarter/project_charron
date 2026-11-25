package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ManifestIssuanceConcurrencyTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    private final UUID reviewerId = UUID.randomUUID();

    private enum Outcome {
        ISSUED,
        CONFLICT
    }

    private Movement createMovement() {

        Movement movement = Movement.plan(
                "TEST-ISSUE-CONCURRENT-" + UUID.randomUUID(),
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
                "Concurrent issuance test review"
        );

        return manifest;
    }

    private ManifestStatus status(UUID manifestId) {

        return lifecycleService
                .getLifecycle(manifestId)
                .status();
    }

    private long countIssuedManifests(UUID movementId) {

        return manifestRepository.findByMovementId(movementId)
                .stream()
                .map(manifest ->
                        lifecycleService.getLifecycle(manifest.id())
                )
                .filter(ManifestLifecycle::isIssued)
                .count();
    }

    private Outcome attemptIssuance(
            UUID manifestId,
            UUID actorId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {

            throw new IllegalStateException(
                    "Timed out waiting for issuance start"
            );
        }

        try {

            ManifestLifecycle lifecycle =
                    lifecycleService.issue(
                            manifestId,
                            actorId,
                            "Concurrent issuance"
                    );

            assertEquals(
                    ManifestStatus.ISSUED,
                    lifecycle.status()
            );

            return Outcome.ISSUED;

        } catch (ManifestLifecycleConflictException exception) {

            return Outcome.CONFLICT;
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
                    () -> attemptIssuance(
                            firstManifestId,
                            UUID.randomUUID(),
                            ready,
                            start
                    )
            );

            Future<Outcome> second = executor.submit(
                    () -> attemptIssuance(
                            secondManifestId,
                            UUID.randomUUID(),
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

    @Test
    void permitsOnlyOneConcurrentIssuanceOfSameManifest()
            throws Exception {

        Movement movement = createMovement();

        MovementManifest manifest =
                generateReviewedManifest(movement.id());

        List<Outcome> outcomes = issueConcurrently(
                manifest.id(),
                manifest.id()
        );

        assertEquals(
                1L,
                outcomes.stream()
                        .filter(outcome -> outcome == Outcome.ISSUED)
                        .count()
        );

        assertEquals(
                1L,
                outcomes.stream()
                        .filter(outcome -> outcome == Outcome.CONFLICT)
                        .count()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(manifest.id())
        );

        assertEquals(
                2,
                eventRepository.findByManifestId(
                        manifest.id()
                ).size()
        );

        assertEquals(
                1L,
                countIssuedManifests(movement.id())
        );
    }

    @Test
    void newestVersionRemainsIssuedAfterConcurrentRequests()
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

        assertEquals(
                Outcome.ISSUED,
                outcomes.getLast()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(newer.id())
        );

        if (outcomes.getFirst() == Outcome.ISSUED) {

            assertEquals(
                    ManifestStatus.SUPERSEDED,
                    status(older.id())
            );

            assertEquals(
                    3,
                    eventRepository.findByManifestId(
                            older.id()
                    ).size()
            );

        } else {

            assertEquals(
                    ManifestStatus.REVIEWED,
                    status(older.id())
            );

            assertEquals(
                    1,
                    eventRepository.findByManifestId(
                            older.id()
                    ).size()
            );
        }

        assertEquals(
                1L,
                countIssuedManifests(movement.id())
        );
    }
}