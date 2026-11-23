
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class JdbcLoadingPlanArrangementRepositoryTest {

    @Autowired
    private JdbcLoadingPlanArrangementRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID loadingPlanId;

    private static final String REQUEST = """
            {
              "cargoSpaceId": "test-space",
              "cargo": [
                {
                  "cargoId": "test-cargo",
                  "name": "Test Pallet",
                  "quantity": 2,
                  "lengthMm": 1200,
                  "widthMm": 1000,
                  "heightMm": 1500,
                  "weightGrams": 500000
                }
              ]
            }
            """;

    private static final String PREVIEW = """
            {
              "cargoSpaceId": "test-space",
              "status": "VALID",
              "reason": "Cargo placed",
              "placements": []
            }
            """;

    @BeforeEach
    void setUp() {
        UUID movementId = UUID.randomUUID();
        UUID equipmentId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        loadingPlanId = UUID.randomUUID();

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
                "ARR-TEST-" + movementId,
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
                "Arrangement Test Trailer",
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
    }

    private LoadingPlanArrangement newArrangement() {
        return LoadingPlanArrangement.create(
                loadingPlanId,
                REQUEST,
                PREVIEW,
                "VALID"
        );
    }

    @Test
    void insertsArrangement() {
        LoadingPlanArrangement arrangement = newArrangement();

        repository.save(arrangement);

        assertTrue(
                repository.findByLoadingPlanId(loadingPlanId)
                        .isPresent()
        );
    }

    @Test
    void retrievesSavedArrangement() throws Exception {
        LoadingPlanArrangement arrangement = newArrangement();

        repository.save(arrangement);

        LoadingPlanArrangement found = repository
                .findByLoadingPlanId(loadingPlanId)
                .orElseThrow();

        assertEquals(arrangement.loadingPlanId(), found.loadingPlanId());
        assertEquals(arrangement.formatVersion(), found.formatVersion());
        assertEquals(arrangement.status(), found.status());
        assertEquals(arrangement.generatedAt(), found.generatedAt());
        assertEquals(arrangement.savedAt(), found.savedAt());
        assertEquals(arrangement.updatedAt(), found.updatedAt());

        // PostgreSQL JSONB normalizes JSON formatting and key order.
        assertEquals(
                objectMapper.readTree(arrangement.requestSnapshot()),
                objectMapper.readTree(found.requestSnapshot())
        );

        assertEquals(
                objectMapper.readTree(arrangement.previewSnapshot()),
                objectMapper.readTree(found.previewSnapshot())
        );
    }

    @Test
    void replacesExistingArrangement() throws Exception {
        LoadingPlanArrangement original = newArrangement();

        repository.save(original);

        String updatedPreview = """
                {
                  "cargoSpaceId": "test-space",
                  "status": "INVALID",
                  "reason": "Cargo does not fit",
                  "placements": []
                }
                """;

        Instant later = original.updatedAt().plusSeconds(5);

        LoadingPlanArrangement replacement =
                new LoadingPlanArrangement(
                        loadingPlanId,
                        1,
                        REQUEST,
                        updatedPreview,
                        "INVALID",
                        later,
                        original.savedAt(),
                        later
                );

        repository.save(replacement);

        LoadingPlanArrangement found = repository
                .findByLoadingPlanId(loadingPlanId)
                .orElseThrow();

        assertEquals("INVALID", found.status());
        assertEquals(original.savedAt(), found.savedAt());
        assertEquals(later, found.updatedAt());
        assertEquals(later, found.generatedAt());

        assertEquals(
                objectMapper.readTree(updatedPreview),
                objectMapper.readTree(found.previewSnapshot())
        );

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM loading_plan_arrangements
                WHERE loading_plan_id = ?
                """,
                Integer.class,
                loadingPlanId
        );

        assertEquals(1, count);
    }

    @Test
    void deletesArrangementWithoutDeletingPlan() {
        repository.save(newArrangement());

        repository.deleteByLoadingPlanId(loadingPlanId);

        assertTrue(
                repository.findByLoadingPlanId(loadingPlanId)
                        .isEmpty()
        );

        Integer planCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM loading_plans
                WHERE id = ?
                """,
                Integer.class,
                loadingPlanId
        );

        assertEquals(1, planCount);
    }

    @Test
    void returnsEmptyForUnknownArrangement() {
        assertTrue(
                repository.findByLoadingPlanId(UUID.randomUUID())
                        .isEmpty()
        );
    }

    @Test
    void rejectsUnknownLoadingPlan() {
        LoadingPlanArrangement invalid =
                LoadingPlanArrangement.create(
                        UUID.randomUUID(),
                        REQUEST,
                        PREVIEW,
                        "VALID"
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(invalid)
        );
    }

    @Test
    void rejectsInvalidJsonSnapshot() {
        LoadingPlanArrangement invalid =
                LoadingPlanArrangement.create(
                        loadingPlanId,
                        "{ invalid json }",
                        PREVIEW,
                        "VALID"
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(invalid)
        );
    }
}
