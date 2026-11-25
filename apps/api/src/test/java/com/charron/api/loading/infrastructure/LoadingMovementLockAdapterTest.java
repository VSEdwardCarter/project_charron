package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.LoadingMovementLock;
import com.charron.api.movement.application.MovementRepository;
import com.charron.api.movement.domain.Movement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LoadingMovementLockAdapterTest {

    @Autowired
    private LoadingMovementLock movementLock;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Movement createMovement() {

        Movement movement = Movement.plan(
                "TEST-LOADING-LOCK-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        return movement;
    }

    private TransactionTemplate transaction() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED
        );

        transaction.setTimeout(10);

        return transaction;
    }

    private TransactionTemplate independentTransaction() {

        TransactionTemplate transaction = transaction();

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        return transaction;
    }

    private void configureLockTimeout() {

        jdbcTemplate.execute(
                "SET LOCAL lock_timeout = '500ms'"
        );
    }

    private void assertMovementLockUnavailable(UUID movementId) {

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> independentTransaction()
                        .executeWithoutResult(status -> {

                            configureLockTimeout();

                            movementLock.acquire(movementId);
                        })
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains("lock timeout"),
                exception.getMessage()
        );
    }

    @Test
    void acquiresLockForExistingMovement() {

        Movement movement = createMovement();

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementLock.acquire(movement.id())
            );
        });
    }

    @Test
    void returnsFalseForUnknownMovement() {

        transaction().executeWithoutResult(status -> {

            assertFalse(
                    movementLock.acquire(UUID.randomUUID())
            );
        });
    }

    @Test
    void rejectsLockAcquisitionWithoutTransaction() {

        assertThrows(
                IllegalTransactionStateException.class,
                () -> movementLock.acquire(UUID.randomUUID())
        );
    }

    @Test
    void permitsRepeatedAcquisitionWithinSameTransaction() {

        Movement movement = createMovement();

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementLock.acquire(movement.id())
            );

            assertTrue(
                    movementLock.acquire(movement.id())
            );
        });
    }

    @Test
    void blocksIndependentTransactionForSameMovement() {

        Movement movement = createMovement();

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementLock.acquire(movement.id())
            );

            assertMovementLockUnavailable(
                    movement.id()
            );
        });
    }

    @Test
    void permitsIndependentMovementLockAcquisition() {

        Movement first = createMovement();
        Movement second = createMovement();

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementLock.acquire(first.id())
            );

            independentTransaction()
                    .executeWithoutResult(independent -> {

                        configureLockTimeout();

                        assertTrue(
                                movementLock.acquire(second.id())
                        );
                    });
        });
    }

    @Test
    void releasesLockAfterTransactionCommit() {

        Movement movement = createMovement();

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementLock.acquire(movement.id())
            );
        });

        independentTransaction()
                .executeWithoutResult(status -> {

                    configureLockTimeout();

                    assertTrue(
                            movementLock.acquire(movement.id())
                    );
                });
    }

    @Test
    void releasesLockAfterTransactionRollback() {

        Movement movement = createMovement();

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementLock.acquire(movement.id())
            );

            status.setRollbackOnly();
        });

        independentTransaction()
                .executeWithoutResult(status -> {

                    configureLockTimeout();

                    assertTrue(
                            movementLock.acquire(movement.id())
                    );
                });
    }

    @Test
    void sharesLockWithMovementRepository() {

        Movement movement = createMovement();

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementRepository.findByIdForUpdate(
                            movement.id()
                    ).isPresent()
            );

            assertMovementLockUnavailable(
                    movement.id()
            );
        });
    }
}