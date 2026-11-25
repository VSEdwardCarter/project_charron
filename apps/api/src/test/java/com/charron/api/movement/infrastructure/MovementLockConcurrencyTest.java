package com.charron.api.movement.infrastructure;

import com.charron.api.movement.domain.Movement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MovementLockConcurrencyTest {

    @Autowired
    private JdbcMovementRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void serializesConcurrentMovementLocks() throws Exception {

        Movement movement = Movement.plan(
                "TEST-LOCK-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        repository.save(movement);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch firstLocked =
                new CountDownLatch(1);

        CountDownLatch releaseFirst =
                new CountDownLatch(1);

        CountDownLatch secondAttempting =
                new CountDownLatch(1);

        try {

            Future<?> first = executor.submit(() -> {

                TransactionTemplate transaction =
                        new TransactionTemplate(transactionManager);

                transaction.executeWithoutResult(status -> {

                    assertTrue(
                            repository.findByIdForUpdate(
                                    movement.id()
                            ).isPresent()
                    );

                    firstLocked.countDown();

                    await(releaseFirst);
                });
            });

            assertTrue(
                    firstLocked.await(5, TimeUnit.SECONDS),
                    "First transaction failed to acquire lock"
            );

            Future<Movement> second = executor.submit(() -> {

                TransactionTemplate transaction =
                        new TransactionTemplate(transactionManager);

                return transaction.execute(status -> {

                    secondAttempting.countDown();

                    return repository
                            .findByIdForUpdate(movement.id())
                            .orElseThrow();
                });
            });

            assertTrue(
                    secondAttempting.await(5, TimeUnit.SECONDS),
                    "Second transaction did not start"
            );

            assertThrows(
                    TimeoutException.class,
                    () -> second.get(
                            500,
                            TimeUnit.MILLISECONDS
                    ),
                    "Second transaction should wait for the lock"
            );

            releaseFirst.countDown();

            first.get(5, TimeUnit.SECONDS);

            Movement restored =
                    second.get(5, TimeUnit.SECONDS);

            assertEquals(movement, restored);

        } finally {

            releaseFirst.countDown();

            executor.shutdownNow();

            if (!executor.awaitTermination(
                    10, TimeUnit.SECONDS
            )) {
                throw new IllegalStateException(
                        "Test transactions did not terminate"
                );
            }

            jdbcTemplate.update(
                    "DELETE FROM movements WHERE id = ?",
                    movement.id()
            );
        }
    }

    private static void await(CountDownLatch latch) {

        try {

            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "Timed out waiting for transaction signal"
                );
            }

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Transaction interrupted",
                    exception
            );
        }
    }
}
