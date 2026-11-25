package com.charron.api.movement.application;

import com.charron.api.loading.application.LoadingPlanArrangementRepository;
import com.charron.api.loading.application.LoadingPlanCargoRepository;
import com.charron.api.loading.application.LoadingPlanRepository;
import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;
import com.charron.api.movement.domain.ManifestChangeType;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ManifestFreshnessIntegrationTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private MovementEquipmentAssignmentRepository assignmentRepository;

    @Autowired
    private LoadingPlanRepository loadingPlanRepository;

    @Autowired
    private LoadingPlanCargoRepository cargoRepository;

    @Autowired
    private LoadingPlanArrangementRepository arrangementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Movement movement;
    private LoadingPlan plan;
    private LoadingPlanCargo cargoLine;
    private LoadingPlanArrangement arrangement;
    private MovementManifest historical;

    private UUID equipmentId;
    private UUID cargoId;

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-FRESHNESS-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        equipmentId = insertEquipment(
                "Test Cargo Trailer",
                "TRAILER"
        );

        assignmentRepository.insert(
                MovementEquipmentAssignment.assign(
                        movement.id(),
                        equipmentId,
                        MovementEquipmentRole.CARGO_CARRIER
                )
        );

        UUID cargoSpaceId = insertCargoSpace(equipmentId);

        plan = LoadingPlan.create(
                movement.id(),
                equipmentId,
                cargoSpaceId
        );

        loadingPlanRepository.insert(plan);

        cargoId = insertCargo();

        cargoLine = LoadingPlanCargo.create(
                plan.id(),
                cargoId,
                2
        );

        cargoRepository.insert(cargoLine);

        arrangement = LoadingPlanArrangement.create(
                plan.id(),
                """
                {
                    "cargo": []
                }
                """,
                """
                {
                    "status": "VALID",
                    "placements": []
                }
                """,
                "VALID"
        );

        arrangementRepository.save(arrangement);

        historical = manifestService.generateAndPersist(
                movement.id()
        );
    }

    private UUID insertEquipment(
            String name,
            String category
    ) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id, name, category
                )
                VALUES (?, ?, ?)
                """,
                id,
                name,
                category
        );

        return id;
    }

    private UUID insertCargoSpace(UUID equipmentId) {

        UUID id = UUID.randomUUID();

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
                id,
                equipmentId,
                "Cargo Compartment " + id,
                "ENCLOSED",
                7000L,
                2400L,
                2500L
        );

        return id;
    }

    private UUID insertCargo() {

        UUID id = UUID.randomUUID();

        Instant now = Instant.now()
                .truncatedTo(ChronoUnit.MICROS);

        jdbcTemplate.update(
                """
                INSERT INTO cargo (
                    id,
                    name,
                    reference,
                    description,
                    weight_grams,
                    length_mm,
                    width_mm,
                    height_mm,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                "Test Pallet",
                "PALLET-" + id,
                "Manifest freshness integration test cargo",
                500000L,
                1200L,
                1000L,
                1500L,
                Timestamp.from(now),
                Timestamp.from(now)
        );

        return id;
    }

    private ManifestFreshnessAssessment assess() {

        return manifestService.assessFreshness(
                historical.id()
        );
    }

    private void assertStale(
            ManifestChangeType... expectedChanges
    ) {

        ManifestFreshnessAssessment assessment = assess();

        assertEquals(
                ManifestFreshnessStatus.STALE,
                assessment.status()
        );

        assertEquals(
                Set.of(expectedChanges),
                assessment.changes()
        );

        assertEquals(
                historical.id(),
                assessment.manifestId()
        );

        assertNull(assessment.reason());
    }

    private void replaceArrangement() {

        Instant later = arrangement.updatedAt()
                .plusSeconds(1);

        LoadingPlanArrangement replacement =
                new LoadingPlanArrangement(
                        plan.id(),
                        arrangement.formatVersion(),
                        arrangement.requestSnapshot(),
                        """
                        {
                            "status": "INVALID",
                            "reason": "Cargo configuration changed",
                            "placements": []
                        }
                        """,
                        "INVALID",
                        later,
                        arrangement.savedAt(),
                        later
                );

        arrangementRepository.save(replacement);
    }

    @Test
    void newlyPersistedManifestIsCurrent() {

        ManifestFreshnessAssessment assessment = assess();

        assertEquals(
                ManifestFreshnessStatus.CURRENT,
                assessment.status()
        );

        assertTrue(assessment.changes().isEmpty());
        assertNull(assessment.reason());

        assertEquals(1, historical.equipment().size());
        assertEquals(1, historical.cargo().size());
        assertEquals(1, historical.loadingPlans().size());
    }

    @Test
    void detectsAdditionalEquipmentAssignment() {

        UUID tractorId = insertEquipment(
                "Test Tractor",
                "TRACTOR"
        );

        assignmentRepository.insert(
                MovementEquipmentAssignment.assign(
                        movement.id(),
                        tractorId,
                        MovementEquipmentRole.POWER_UNIT
                )
        );

        assertStale(
                ManifestChangeType.EQUIPMENT
        );
    }

    @Test
    void detectsPersistedEquipmentNameChange() {

        jdbcTemplate.update(
                """
                UPDATE equipment
                SET name = ?
                WHERE id = ?
                """,
                "Renamed Test Trailer",
                equipmentId
        );

        assertStale(
                ManifestChangeType.EQUIPMENT
        );
    }

    @Test
    void detectsPersistedCargoQuantityChange() {

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(5)
        );

        assertStale(
                ManifestChangeType.CARGO
        );
    }

    @Test
    void detectsPersistedCargoWeightChange() {

        jdbcTemplate.update(
                """
                UPDATE cargo
                SET weight_grams = ?
                WHERE id = ?
                """,
                750000L,
                cargoId
        );

        assertStale(
                ManifestChangeType.CARGO
        );
    }

    @Test
    void detectsPersistedCargoRemoval() {

        cargoRepository.deleteById(
                cargoLine.id()
        );

        assertStale(
                ManifestChangeType.CARGO
        );
    }

    @Test
    void detectsPersistedLoadingArrangementChange() {

        replaceArrangement();

        assertStale(
                ManifestChangeType.LOADING_PLANS
        );
    }

    @Test
    void returnsUnverifiableWhenArrangementRemoved() {

        arrangementRepository.deleteByLoadingPlanId(
                plan.id()
        );

        ManifestFreshnessAssessment assessment = assess();

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                assessment.status()
        );

        assertEquals(
                "No saved loading arrangement for plan: "
                        + plan.id(),
                assessment.reason()
        );

        assertTrue(assessment.changes().isEmpty());
        assertFalse(assessment.isCurrent());
    }

    @Test
    void returnsUnverifiableForNewPlanWithoutArrangement() {

        UUID additionalCargoSpace =
                insertCargoSpace(equipmentId);

        LoadingPlan additionalPlan =
                LoadingPlan.create(
                        movement.id(),
                        equipmentId,
                        additionalCargoSpace
                );

        loadingPlanRepository.insert(additionalPlan);

        ManifestFreshnessAssessment assessment = assess();

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                assessment.status()
        );

        assertEquals(
                "No saved loading arrangement for plan: "
                        + additionalPlan.id(),
                assessment.reason()
        );
    }

    @Test
    void returnsCurrentWhenOriginalQuantityRestored() {

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(5)
        );

        assertStale(
                ManifestChangeType.CARGO
        );

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(2)
        );

        assertTrue(
                assess().isCurrent()
        );
    }

    @Test
    void detectsMultiplePersistedOperationalChanges() {

        jdbcTemplate.update(
                """
                UPDATE equipment
                SET name = ?
                WHERE id = ?
                """,
                "Renamed Trailer",
                equipmentId
        );

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(5)
        );

        replaceArrangement();

        assertStale(
                ManifestChangeType.EQUIPMENT,
                ManifestChangeType.CARGO,
                ManifestChangeType.LOADING_PLANS
        );
    }

    @Test
    void preservesHistoricalManifestSnapshot() {

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(5)
        );

        assertStale(
                ManifestChangeType.CARGO
        );

        MovementManifest restored =
                manifestRepository.findById(
                        historical.id()
                ).orElseThrow();

        assertEquals(historical, restored);

        assertEquals(
                2,
                restored.cargo()
                        .getFirst()
                        .quantity()
        );
    }

    @Test
    void assessmentDoesNotConsumeManifestVersion() {

        assess();
        assess();
        assess();

        assertEquals(
                1,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );

        assertEquals(
                1,
                manifestRepository.findByMovementId(
                        movement.id()
                ).size()
        );
    }

    @Test
    void freshnessAssessmentDoesNotModifyLifecycle() {

        lifecycleService.review(
                historical.id(),
                UUID.randomUUID(),
                "Integration test review"
        );

        var originalEvents =
                eventRepository.findByManifestId(
                        historical.id()
                );

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(5)
        );

        assertStale(
                ManifestChangeType.CARGO
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycleService.getLifecycle(
                        historical.id()
                ).status()
        );

        assertEquals(
                originalEvents,
                eventRepository.findByManifestId(
                        historical.id()
                )
        );
    }

    @Test
    void issuedManifestCanBecomeStaleWithoutLosingIssuedStatus() {

        UUID actorId = UUID.randomUUID();

        lifecycleService.review(
                historical.id(),
                actorId,
                "Reviewed"
        );

        lifecycleService.issue(
                historical.id(),
                actorId,
                "Issued"
        );

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(5)
        );

        assertStale(
                ManifestChangeType.CARGO
        );

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService.getLifecycle(
                        historical.id()
                ).status()
        );

        assertEquals(
                2,
                eventRepository.findByManifestId(
                        historical.id()
                ).size()
        );
    }

    @Test
    void newlyGeneratedVersionReflectsCurrentOperationalData() {

        cargoRepository.updateQuantity(
                cargoLine.withQuantity(5)
        );

        assertStale(
                ManifestChangeType.CARGO
        );

        MovementManifest replacement =
                manifestService.generateAndPersist(
                        movement.id()
                );

        assertEquals(
                2,
                replacement.version()
        );

        assertEquals(
                5,
                replacement.cargo()
                        .getFirst()
                        .quantity()
        );

        assertTrue(
                manifestService.assessFreshness(
                        replacement.id()
                ).isCurrent()
        );

        assertTrue(
                assess().isStale()
        );
    }
}