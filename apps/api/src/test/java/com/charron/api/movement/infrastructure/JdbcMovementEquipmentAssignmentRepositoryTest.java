
package com.charron.api.movement.infrastructure;

import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcMovementEquipmentAssignmentRepositoryTest {

    @Autowired
    private JdbcMovementEquipmentAssignmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID movementId;
    private UUID equipmentId;

    @BeforeEach
    void setUp() {
        movementId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();

        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        jdbcTemplate.update(
                """
                INSERT INTO movements (
                    id, reference, origin, destination,
                    planned_departure, planned_arrival,
                    status, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                movementId,
                "TEST-" + movementId,
                "Denver, CO",
                "Dallas, TX",
                Timestamp.from(now.plus(1, ChronoUnit.DAYS)),
                Timestamp.from(now.plus(2, ChronoUnit.DAYS)),
                "PLANNED",
                Timestamp.from(now),
                Timestamp.from(now)
        );

        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id, name, category
                )
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "Test Tractor",
                "TRACTOR"
        );
    }

    private MovementEquipmentAssignment assignment(
            MovementEquipmentRole role
    ) {
        return new MovementEquipmentAssignment(
                UUID.randomUUID(),
                movementId,
                equipmentId,
                role,
                Instant.now().truncatedTo(ChronoUnit.MICROS)
        );
    }

    @Test
    void insertsAssignment() {
        MovementEquipmentAssignment assignment =
                assignment(MovementEquipmentRole.POWER_UNIT);

        repository.insert(assignment);

        assertTrue(repository.findById(assignment.id()).isPresent());
    }

    @Test
    void retrievesAssignmentById() {
        MovementEquipmentAssignment assignment =
                assignment(MovementEquipmentRole.POWER_UNIT);

        repository.insert(assignment);

        MovementEquipmentAssignment found =
                repository.findById(assignment.id()).orElseThrow();

        assertEquals(assignment, found);
    }

    @Test
    void retrievesAssignmentsByMovementId() {
        repository.insert(
                assignment(MovementEquipmentRole.POWER_UNIT)
        );

        repository.insert(
                assignment(MovementEquipmentRole.CARGO_CARRIER)
        );

        var results = repository.findByMovementId(movementId);

        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(
                a -> a.movementId().equals(movementId)
        ));
    }

    @Test
    void detectsExistingAssignment() {
        MovementEquipmentAssignment assignment =
                assignment(MovementEquipmentRole.POWER_UNIT);

        repository.insert(assignment);

        assertTrue(
                repository.existsByMovementAndEquipmentAndRole(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT
                )
        );

        assertFalse(
                repository.existsByMovementAndEquipmentAndRole(
                        movementId,
                        equipmentId,
                        MovementEquipmentRole.SUPPORT
                )
        );
    }

    @Test
    void rejectsDuplicateAssignment() {
        repository.insert(
                assignment(MovementEquipmentRole.POWER_UNIT)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(
                        assignment(MovementEquipmentRole.POWER_UNIT)
                )
        );
    }

    @Test
    void rejectsUnknownMovement() {
        MovementEquipmentAssignment invalid =
                new MovementEquipmentAssignment(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        equipmentId,
                        MovementEquipmentRole.POWER_UNIT,
                        Instant.now()
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(invalid)
        );
    }

    @Test
    void rejectsUnknownEquipment() {
        MovementEquipmentAssignment invalid =
                new MovementEquipmentAssignment(
                        UUID.randomUUID(),
                        movementId,
                        UUID.randomUUID(),
                        MovementEquipmentRole.POWER_UNIT,
                        Instant.now()
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(invalid)
        );
    }
}
