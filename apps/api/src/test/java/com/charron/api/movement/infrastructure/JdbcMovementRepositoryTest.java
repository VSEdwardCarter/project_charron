
package com.charron.api.movement.infrastructure;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;
import org.junit.jupiter.api.Test;
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
}
