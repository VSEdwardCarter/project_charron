
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.domain.LoadingPlanCargo;
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
class JdbcLoadingPlanCargoRepositoryTest {

    @Autowired
    private JdbcLoadingPlanCargoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID loadingPlanId;
    private UUID cargoId;

    @BeforeEach
    void setUp() {
        UUID movementId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        loadingPlanId = UUID.randomUUID();
        cargoId = UUID.randomUUID();

        Instant now = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

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
                "CARGO-TEST-" + movementId,
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
                INSERT INTO equipment (id, name, category)
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "Test Cargo Trailer",
                "TRAILER"
        );

        jdbcTemplate.update(
                """
                INSERT INTO equipment_cargo_spaces (
                    id, equipment_id, name, space_type,
                    usable_length_mm, usable_width_mm,
                    usable_height_mm
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                cargoSpaceId,
                equipmentId,
                "Main Cargo Compartment",
                "ENCLOSED",
                7000L,
                2400L,
                2500L
        );

        jdbcTemplate.update(
                """
                INSERT INTO loading_plans (
                    id, movement_id, equipment_id,
                    cargo_space_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                loadingPlanId,
                movementId,
                equipmentId,
                cargoSpaceId,
                Timestamp.from(now),
                Timestamp.from(now)
        );

        jdbcTemplate.update(
                """
                INSERT INTO cargo (
                    id, name, reference, description,
                    weight_grams, length_mm, width_mm,
                    height_mm, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                cargoId,
                "Test Pallet",
                "PALLET-" + cargoId,
                "Integration test cargo",
                500000L,
                1200L,
                1000L,
                1500L,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private LoadingPlanCargo newCargoLine(int quantity) {
        return LoadingPlanCargo.create(
                loadingPlanId,
                cargoId,
                quantity
        );
    }

    @Test
    void insertsCargoLine() {
        LoadingPlanCargo line = newCargoLine(3);

        repository.insert(line);

        assertTrue(repository.findById(line.id()).isPresent());
    }

    @Test
    void retrievesCargoLineById() {
        LoadingPlanCargo line = newCargoLine(3);
        repository.insert(line);

        LoadingPlanCargo found =
                repository.findById(line.id()).orElseThrow();

        assertEquals(line, found);
    }

    @Test
    void listsCargoForLoadingPlan() {
        LoadingPlanCargo line = newCargoLine(3);
        repository.insert(line);

        var results =
                repository.findByLoadingPlanId(loadingPlanId);

        assertEquals(1, results.size());
        assertEquals(line, results.getFirst());
    }

    @Test
    void detectsExistingCargoAssignment() {
        assertFalse(
                repository.existsByLoadingPlanAndCargo(
                        loadingPlanId,
                        cargoId
                )
        );

        repository.insert(newCargoLine(3));

        assertTrue(
                repository.existsByLoadingPlanAndCargo(
                        loadingPlanId,
                        cargoId
                )
        );
    }

    @Test
    void updatesCargoQuantity() {
        LoadingPlanCargo original = newCargoLine(2);
        repository.insert(original);

        LoadingPlanCargo updated =
                original.withQuantity(5);

        repository.updateQuantity(updated);

        LoadingPlanCargo found =
                repository.findById(original.id()).orElseThrow();

        assertEquals(5, found.quantity());
        assertEquals(original.id(), found.id());
        assertEquals(original.createdAt(), found.createdAt());
    }

    @Test
    void deletesCargoLine() {
        LoadingPlanCargo line = newCargoLine(3);
        repository.insert(line);

        repository.deleteById(line.id());

        assertTrue(repository.findById(line.id()).isEmpty());
    }

    @Test
    void rejectsDuplicateCargoAssignment() {
        repository.insert(newCargoLine(2));

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(newCargoLine(4))
        );
    }

    @Test
    void rejectsUnknownCatalogCargo() {
        LoadingPlanCargo invalid = LoadingPlanCargo.create(
                loadingPlanId,
                UUID.randomUUID(),
                1
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.insert(invalid)
        );
    }

    @Test
    void returnsEmptyForUnknownCargoLine() {
        assertTrue(
                repository.findById(UUID.randomUUID()).isEmpty()
        );
    }
}
