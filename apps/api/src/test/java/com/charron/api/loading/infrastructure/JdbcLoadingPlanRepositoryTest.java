
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.domain.LoadingPlan;
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
class JdbcLoadingPlanRepositoryTest {

    @Autowired
    private JdbcLoadingPlanRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID movementId;
    private UUID equipmentId;
    private UUID cargoSpaceId;

    @BeforeEach
    void setUp() {
        movementId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();
        cargoSpaceId = UUID.randomUUID();

        Instant now = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        // Create a movement for the loading plan.
        jdbcTemplate.update(
                """
                INSERT INTO movements (
                    id,
                    reference,
                    origin,
                    destination,
                    planned_departure,
                    planned_arrival,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                movementId,
                "LOAD-TEST-" + movementId,
                "Denver, CO",
                "Dallas, TX",
                Timestamp.from(now.plus(1, ChronoUnit.DAYS)),
                Timestamp.from(now.plus(2, ChronoUnit.DAYS)),
                "PLANNED",
                Timestamp.from(now),
                Timestamp.from(now)
        );

        // Create registered equipment.
        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id,
                    name,
                    category
                )
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "Test Loading Trailer",
                "TRAILER"
        );

        // Create a cargo space belonging to the equipment.
        jdbcTemplate.update(
                """
                INSERT INTO equipment_cargo_spaces (
                    id,
                    equipment_id,
                    name,
                    space_type,
                    usable_length_mm,
                    usable_width_mm,
                    usable_height_mm
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                cargoSpaceId,
                equipmentId,
                "Main Cargo Deck",
                "OPEN_DECK",
                12000L,
                2500L,
                3000L
        );
    }

    private LoadingPlan newPlan() {
        return LoadingPlan.create(
                movementId,
                equipmentId,
                cargoSpaceId
        );
    }

    @Test
    void insertsLoadingPlan() {
        LoadingPlan plan = newPlan();

        repository.insert(plan);

        assertTrue(
                repository.findById(plan.id()).isPresent()
        );
    }

    @Test
    void retrievesLoadingPlanById() {
        LoadingPlan plan = newPlan();

        repository.insert(plan);

        LoadingPlan found = repository
                .findById(plan.id())
                .orElseThrow();

        assertEquals(plan, found);
    }

    @Test
    void retrievesLoadingPlansByMovementId() {
        LoadingPlan plan = newPlan();

        repository.insert(plan);

        var results =
                repository.findByMovementId(movementId);

        assertEquals(1, results.size());
        assertEquals(plan, results.getFirst());
    }

    @Test
    void detectsExistingLoadingPlan() {
        assertFalse(
                repository.existsByMovementAndCargoSpace(
                        movementId,
                        cargoSpaceId
                )
        );

        repository.insert(newPlan());

        assertTrue(
                repository.existsByMovementAndCargoSpace(
                        movementId,
                        cargoSpaceId
                )
        );
    }

    @Test
    void returnsEmptyForUnknownLoadingPlan() {
        assertTrue(
                repository.findById(UUID.randomUUID())
                        .isEmpty()
        );
    }

    @Test
    void rejectsDuplicateMovementCargoSpace() {
        repository.insert(newPlan());

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(newPlan())
        );
    }

    @Test
    void rejectsUnknownMovement() {
        LoadingPlan invalid = LoadingPlan.create(
                UUID.randomUUID(),
                equipmentId,
                cargoSpaceId
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(invalid)
        );
    }

    @Test
    void rejectsUnknownEquipment() {
        LoadingPlan invalid = LoadingPlan.create(
                movementId,
                UUID.randomUUID(),
                cargoSpaceId
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(invalid)
        );
    }

    @Test
    void rejectsUnknownCargoSpace() {
        LoadingPlan invalid = LoadingPlan.create(
                movementId,
                equipmentId,
                UUID.randomUUID()
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(invalid)
        );
    }
}
