
package com.charron.api.movement.infrastructure;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JdbcMovementRepositoryTest {

    @Autowired
    private JdbcMovementRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Movement createMovement() {
        return Movement.plan(
                "TEST-MOV-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );
    }

    private void deleteMovement(Movement movement) {
        jdbcTemplate.update(
                "DELETE FROM movements WHERE id = ?",
                movement.id()
        );
    }

    private boolean transition(
            Movement movement,
            MovementStatus expected,
            MovementStatus target,
            Instant timestamp
    ) {
        return repository.updateStatus(
                movement.id(),
                expected,
                target,
                timestamp
        );
    }

    @Test
    void insertsAndRetrievesMovement() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            Movement restored = repository.findById(movement.id())
                    .orElseThrow();

            assertEquals(movement.id(), restored.id());
            assertEquals(movement.reference(), restored.reference());
            assertEquals(movement.origin(), restored.origin());
            assertEquals(movement.destination(), restored.destination());
            assertEquals(movement.plannedDeparture(), restored.plannedDeparture());
            assertEquals(movement.plannedArrival(), restored.plannedArrival());
            assertEquals(MovementStatus.PLANNED, restored.status());
            assertEquals(movement.createdAt(), restored.createdAt());
            assertEquals(movement.updatedAt(), restored.updatedAt());
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void returnsEmptyForUnknownMovement() {
        assertTrue(
                repository.findById(UUID.randomUUID()).isEmpty()
        );
    }

    @Test
    void findAllIncludesSavedMovement() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            assertTrue(
                    repository.findAll().stream()
                            .anyMatch(item ->
                                    item.id().equals(movement.id())
                            )
            );
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void detectsExistingMovementReference() {
        Movement movement = createMovement();

        assertFalse(
                repository.existsByReference(movement.reference())
        );

        try {
            repository.save(movement);

            assertTrue(
                    repository.existsByReference(movement.reference())
            );
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void rejectsDuplicateMovementReference() {
        Movement first = createMovement();

        Movement second = Movement.plan(
                first.reference(),
                "Colorado Springs, CO",
                "Houston, TX",
                Instant.parse("2026-12-03T08:00:00Z"),
                Instant.parse("2026-12-04T08:00:00Z")
        );

        try {
            repository.save(first);

            assertThrows(
                    DuplicateKeyException.class,
                    () -> repository.save(second)
            );

            assertTrue(repository.findById(second.id()).isEmpty());
        } finally {
            deleteMovement(first);
            deleteMovement(second);
        }
    }

    @Test
    void persistsDispatchStatus() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            Instant updatedAt = movement.updatedAt().plusSeconds(1);

            assertTrue(transition(
                    movement,
                    MovementStatus.PLANNED,
                    MovementStatus.DISPATCHED,
                    updatedAt
            ));

            Movement restored = repository.findById(movement.id())
                    .orElseThrow();

            assertEquals(MovementStatus.DISPATCHED, restored.status());
            assertEquals(updatedAt, restored.updatedAt());
            assertEquals(movement.id(), restored.id());
            assertEquals(movement.createdAt(), restored.createdAt());
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void persistsCompleteMovementLifecycle() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            Instant t1 = movement.updatedAt().plusSeconds(1);
            Instant t2 = t1.plusSeconds(1);
            Instant t3 = t2.plusSeconds(1);

            assertTrue(transition(
                    movement,
                    MovementStatus.PLANNED,
                    MovementStatus.DISPATCHED,
                    t1
            ));

            assertTrue(transition(
                    movement,
                    MovementStatus.DISPATCHED,
                    MovementStatus.IN_TRANSIT,
                    t2
            ));

            assertTrue(transition(
                    movement,
                    MovementStatus.IN_TRANSIT,
                    MovementStatus.COMPLETED,
                    t3
            ));

            Movement restored = repository.findById(movement.id())
                    .orElseThrow();

            assertEquals(MovementStatus.COMPLETED, restored.status());
            assertEquals(t3, restored.updatedAt());
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void rejectsIncorrectExpectedStatus() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            boolean updated = transition(
                    movement,
                    MovementStatus.DISPATCHED,
                    MovementStatus.IN_TRANSIT,
                    movement.updatedAt().plusSeconds(1)
            );

            assertFalse(updated);

            assertEquals(
                    MovementStatus.PLANNED,
                    repository.findById(movement.id()).orElseThrow().status()
            );
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void rejectsRepeatedDispatch() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            Instant t1 = movement.updatedAt().plusSeconds(1);
            Instant t2 = t1.plusSeconds(1);

            assertTrue(transition(
                    movement,
                    MovementStatus.PLANNED,
                    MovementStatus.DISPATCHED,
                    t1
            ));

            assertFalse(transition(
                    movement,
                    MovementStatus.PLANNED,
                    MovementStatus.DISPATCHED,
                    t2
            ));

            Movement restored = repository.findById(movement.id())
                    .orElseThrow();

            assertEquals(MovementStatus.DISPATCHED, restored.status());
            assertEquals(t1, restored.updatedAt());
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void returnsFalseForNonexistentMovement() {
        assertFalse(
                repository.updateStatus(
                        UUID.randomUUID(),
                        MovementStatus.PLANNED,
                        MovementStatus.DISPATCHED,
                        Instant.now()
                )
        );
    }

    @Test
    void rejectsInvalidTransitionBeforeDatabaseUpdate() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> transition(
                            movement,
                            MovementStatus.PLANNED,
                            MovementStatus.COMPLETED,
                            movement.updatedAt().plusSeconds(1)
                    )
            );

            assertEquals(
                    MovementStatus.PLANNED,
                    repository.findById(movement.id()).orElseThrow().status()
            );
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void preventsTimestampFromMovingBackward() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            Instant t1 = movement.updatedAt().plusSeconds(10);
            Instant earlier = movement.updatedAt().plusSeconds(5);

            assertTrue(transition(
                    movement,
                    MovementStatus.PLANNED,
                    MovementStatus.DISPATCHED,
                    t1
            ));

            assertFalse(transition(
                    movement,
                    MovementStatus.DISPATCHED,
                    MovementStatus.IN_TRANSIT,
                    earlier
            ));

            Movement restored = repository.findById(movement.id())
                    .orElseThrow();

            assertEquals(MovementStatus.DISPATCHED, restored.status());
            assertEquals(t1, restored.updatedAt());
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void persistsCancellation() {
        Movement movement = createMovement();

        try {
            repository.save(movement);

            assertTrue(transition(
                    movement,
                    MovementStatus.PLANNED,
                    MovementStatus.CANCELLED,
                    movement.updatedAt().plusSeconds(1)
            ));

            assertEquals(
                    MovementStatus.CANCELLED,
                    repository.findById(movement.id()).orElseThrow().status()
            );
        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void retrievesMovementWithLockInsideTransaction() {

        Movement movement = createMovement();

        try {
            repository.save(movement);

            TransactionTemplate transaction =
                    new TransactionTemplate(transactionManager);

            transaction.executeWithoutResult(status -> {

                Movement locked = repository
                        .findByIdForUpdate(movement.id())
                        .orElseThrow();

                assertEquals(movement, locked);
            });

        } finally {
            deleteMovement(movement);
        }
    }

    @Test
    void rejectsLockWithoutActiveTransaction() {

        assertThrows(
                IllegalTransactionStateException.class,
                () -> repository.findByIdForUpdate(UUID.randomUUID())
        );
    }

    @Test
    void returnsEmptyWhenLockingUnknownMovement() {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {

            assertTrue(
                    repository.findByIdForUpdate(
                            UUID.randomUUID()
                    ).isEmpty()
            );
        });
    }
}
