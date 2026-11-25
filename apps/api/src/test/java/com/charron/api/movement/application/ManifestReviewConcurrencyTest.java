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
class ManifestReviewConcurrencyTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    private enum ReviewOutcome {
        REVIEWED,
        CONFLICT
    }

    @Test
    void permitsOnlyOneConcurrentReview() throws Exception {

        Movement movement = Movement.plan(
                "TEST-REVIEW-CONCURRENT-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        MovementManifest manifest =
                manifestService.generateAndPersist(
                        movement.id()
                );

        UUID firstActor = UUID.randomUUID();
        UUID secondActor = UUID.randomUUID();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {

            Future<ReviewOutcome> first =
                    executor.submit(() -> attemptReview(
                            manifest.id(),
                            firstActor,
                            ready,
                            start
                    ));

            Future<ReviewOutcome> second =
                    executor.submit(() -> attemptReview(
                            manifest.id(),
                            secondActor,
                            ready,
                            start
                    ));

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Review requests did not become ready"
            );

            start.countDown();

            List<ReviewOutcome> outcomes = List.of(
                    first.get(30, TimeUnit.SECONDS),
                    second.get(30, TimeUnit.SECONDS)
            );

            assertEquals(
                    1L,
                    outcomes.stream()
                            .filter(outcome ->
                                    outcome == ReviewOutcome.REVIEWED)
                            .count()
            );

            assertEquals(
                    1L,
                    outcomes.stream()
                            .filter(outcome ->
                                    outcome == ReviewOutcome.CONFLICT)
                            .count()
            );

            ManifestLifecycle lifecycle =
                    lifecycleService.getLifecycle(manifest.id());

            assertEquals(
                    ManifestStatus.REVIEWED,
                    lifecycle.status()
            );

            var events =
                    eventRepository.findByManifestId(
                            manifest.id()
                    );

            assertEquals(1, events.size());

            var review = events.getFirst();

            assertEquals(
                    ManifestStatus.DRAFT,
                    review.previousStatus()
            );

            assertEquals(
                    ManifestStatus.REVIEWED,
                    review.newStatus()
            );

            assertEquals(
                    outcomes.getFirst() == ReviewOutcome.REVIEWED
                            ? firstActor
                            : secondActor,
                    review.actorId()
            );

        } finally {

            start.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            30, TimeUnit.SECONDS
                    ),
                    "Concurrent review requests did not terminate"
            );
        }
    }

    private ReviewOutcome attemptReview(
            UUID manifestId,
            UUID actorId,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {

        ready.countDown();

        if (!start.await(10, TimeUnit.SECONDS)) {

            throw new IllegalStateException(
                    "Timed out waiting for review start"
            );
        }

        try {

            ManifestLifecycle lifecycle =
                    lifecycleService.review(
                            manifestId,
                            actorId,
                            "Concurrent review test"
                    );

            assertEquals(
                    ManifestStatus.REVIEWED,
                    lifecycle.status()
            );

            return ReviewOutcome.REVIEWED;

        } catch (ManifestLifecycleConflictException exception) {

            return ReviewOutcome.CONFLICT;
        }
    }
}