package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MovementManifestConcurrencyTest {

    private static final int REQUEST_COUNT = 8;

    @Autowired
    private MovementManifestService service;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void allocatesUniqueSequentialVersionsConcurrently()
            throws Exception {

        Movement movement = Movement.plan(
                "TEST-MANIFEST-CONCURRENT-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        ExecutorService executor =
                Executors.newFixedThreadPool(REQUEST_COUNT);

        CountDownLatch ready =
                new CountDownLatch(REQUEST_COUNT);

        CountDownLatch start =
                new CountDownLatch(1);

        List<Future<MovementManifest>> futures =
                new ArrayList<>();

        try {

            for (int i = 0; i < REQUEST_COUNT; i++) {

                futures.add(executor.submit(() -> {

                    ready.countDown();

                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                "Timed out waiting for start signal"
                        );
                    }

                    return service.generateAndPersist(
                            movement.id()
                    );
                }));
            }

            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Not all concurrent requests became ready"
            );

            start.countDown();

            List<MovementManifest> generated =
                    new ArrayList<>();

            for (Future<MovementManifest> future : futures) {

                generated.add(
                        future.get(30, TimeUnit.SECONDS)
                );
            }

            List<Integer> actualVersions = generated.stream()
                    .map(MovementManifest::version)
                    .sorted()
                    .toList();

            List<Integer> expectedVersions =
                    IntStream.rangeClosed(1, REQUEST_COUNT)
                            .boxed()
                            .toList();

            assertEquals(
                    expectedVersions,
                    actualVersions
            );

            assertEquals(
                    REQUEST_COUNT,
                    generated.stream()
                            .map(MovementManifest::id)
                            .distinct()
                            .count()
            );

            assertEquals(
                    REQUEST_COUNT,
                    manifestRepository.findLatestVersion(
                            movement.id()
                    )
            );

            List<MovementManifest> history =
                    manifestRepository.findByMovementId(
                            movement.id()
                    );

            assertEquals(
                    REQUEST_COUNT,
                    history.size()
            );

            assertEquals(
                    expectedVersions.reversed(),
                    history.stream()
                            .map(MovementManifest::version)
                            .toList()
            );

            for (MovementManifest manifest : generated) {

                assertEquals(
                        manifest,
                        manifestRepository.findById(
                                manifest.id()
                        ).orElseThrow()
                );
            }

        } finally {

            start.countDown();

            executor.shutdownNow();

            assertTrue(
                    executor.awaitTermination(
                            30, TimeUnit.SECONDS
                    ),
                    "Concurrent requests did not terminate"
            );
        }
    }
}