package com.charron.api.movement.application;

import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.loading.application.LoadingPlanCargoService;
import com.charron.api.loading.application.LoadingPlanArrangementService;
import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanCargo;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementEquipmentRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OperationalMovementLockConcurrencyTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementEquipmentAssignmentService assignmentService;

    @Autowired
    private LoadingPlanService loadingPlanService;

    @Autowired
    private LoadingPlanCargoService cargoService;

    @Autowired
    private LoadingPlanArrangementService arrangementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Movement movement;

    private UUID equipmentId;
    private UUID cargoSpaceId;
    private UUID cargoId;

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-OPERATIONAL-LOCK-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        equipmentId = UUID.randomUUID();
        cargoSpaceId = UUID.randomUUID();
        cargoId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id, name, category
                )
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "Concurrency Test Trailer",
                "TRAILER"
        );

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
                "Concurrency Test Cargo Space",
                "ENCLOSED",
                7000L,
                2400L,
                2500L
        );

        jdbcTemplate.update(
                """
                INSERT INTO cargo (
                    id,
                    name,
                    weight_grams,
                    length_mm,
                    width_mm,
                    height_mm
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                cargoId,
                "Concurrency Test Pallet",
                500000L,
                1200L,
                1000L,
                1500L
        );
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

    private void assertBlockedByMovementLock(
            Runnable operation
    ) {

        transaction().executeWithoutResult(status -> {

            assertTrue(
                    movementRepository.findByIdForUpdate(
                            movement.id()
                    ).isPresent()
            );

            DataAccessException exception = assertThrows(
                    DataAccessException.class,
                    () -> independentTransaction()
                            .executeWithoutResult(independent -> {

                                jdbcTemplate.execute(
                                        "SET LOCAL lock_timeout = '500ms'"
                                );

                                operation.run();
                            })
            );

            SQLException sqlException = assertInstanceOf(
                    SQLException.class,
                    exception.getMostSpecificCause()
            );

            assertEquals(
                    "55P03",
                    sqlException.getSQLState()
            );
        });
    }

    private void assignEquipment() {

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
        );
    }

    private LoadingPlan createLoadingPlan() {

        assignEquipment();

        return loadingPlanService.create(
                movement.id(),
                equipmentId,
                cargoSpaceId
        );
    }

    private LoadingPlanCargo addCargo(
            LoadingPlan plan,
            int quantity
    ) {

        return cargoService.addCargo(
                plan.id(),
                cargoId,
                quantity
        );
    }

    private long countLoadingPlans() {

        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM loading_plans
                WHERE movement_id = ?
                """,
                Long.class,
                movement.id()
        );

        assertNotNull(count);

        return count;
    }

    private String requestSnapshot(int quantity) {

        return """
                {
                  "cargoSpaceId": "%s",
                  "cargo": [
                    {
                      "cargoId": "%s",
                      "name": "Concurrency Test Pallet",
                      "quantity": %d,
                      "lengthMm": 1200,
                      "widthMm": 1000,
                      "heightMm": 1500,
                      "weightGrams": 500000
                    }
                  ]
                }
                """.formatted(
                cargoSpaceId,
                cargoId,
                quantity
        );
    }

    private String previewSnapshot(String status) {

        return """
                {
                  "cargoSpaceId": "%s",
                  "status": "%s",
                  "reason": "Concurrency test",
                  "placements": []
                }
                """.formatted(
                cargoSpaceId,
                status
        );
    }

    @Test
    void equipmentAssignmentWaitsForParentMovementLock() {

        assertTrue(
                assignmentService.findByMovementId(
                        movement.id()
                ).isEmpty()
        );

        assertBlockedByMovementLock(
                this::assignEquipment
        );

        assertTrue(
                assignmentService.findByMovementId(
                        movement.id()
                ).isEmpty()
        );

        assignEquipment();

        assertEquals(
                1,
                assignmentService.findByMovementId(
                        movement.id()
                ).size()
        );
    }

    @Test
    void loadingPlanCreationWaitsForParentMovementLock() {

        assignEquipment();

        assertEquals(0L, countLoadingPlans());

        assertBlockedByMovementLock(
                () -> loadingPlanService.create(
                        movement.id(),
                        equipmentId,
                        cargoSpaceId
                )
        );

        assertEquals(0L, countLoadingPlans());

        loadingPlanService.create(
                movement.id(),
                equipmentId,
                cargoSpaceId
        );

        assertEquals(1L, countLoadingPlans());
    }

    @Test
    void cargoAdditionWaitsForParentMovementLock() {

        LoadingPlan plan = createLoadingPlan();

        assertTrue(
                cargoService.getCargo(plan.id()).isEmpty()
        );

        assertBlockedByMovementLock(
                () -> cargoService.addCargo(
                        plan.id(),
                        cargoId,
                        3
                )
        );

        assertTrue(
                cargoService.getCargo(plan.id()).isEmpty()
        );

        addCargo(plan, 3);

        assertEquals(
                1,
                cargoService.getCargo(plan.id()).size()
        );

        assertEquals(
                3,
                cargoService.getCargo(plan.id())
                        .getFirst()
                        .quantity()
        );
    }

    @Test
    void cargoQuantityUpdateWaitsForParentMovementLock() {

        LoadingPlan plan = createLoadingPlan();

        LoadingPlanCargo line = addCargo(plan, 3);

        assertBlockedByMovementLock(
                () -> cargoService.updateQuantity(
                        plan.id(),
                        line.id(),
                        5
                )
        );

        assertEquals(
                3,
                cargoService.getCargo(plan.id())
                        .getFirst()
                        .quantity()
        );

        cargoService.updateQuantity(
                plan.id(),
                line.id(),
                5
        );

        assertEquals(
                5,
                cargoService.getCargo(plan.id())
                        .getFirst()
                        .quantity()
        );
    }

    @Test
    void cargoRemovalWaitsForParentMovementLock() {

        LoadingPlan plan = createLoadingPlan();

        LoadingPlanCargo line = addCargo(plan, 3);

        assertBlockedByMovementLock(
                () -> cargoService.removeCargo(
                        plan.id(),
                        line.id()
                )
        );

        assertEquals(
                1,
                cargoService.getCargo(plan.id()).size()
        );

        assertEquals(
                line.id(),
                cargoService.getCargo(plan.id())
                        .getFirst()
                        .id()
        );

        cargoService.removeCargo(
                plan.id(),
                line.id()
        );

        assertTrue(
                cargoService.getCargo(plan.id()).isEmpty()
        );
    }

    @Test
    void arrangementCreationWaitsForParentMovementLock() {

        LoadingPlan plan = createLoadingPlan();

        addCargo(plan, 3);

        assertTrue(
                arrangementService.findByLoadingPlanId(
                        plan.id()
                ).isEmpty()
        );

        assertBlockedByMovementLock(
                () -> arrangementService.save(
                        plan.id(),
                        requestSnapshot(3),
                        previewSnapshot("VALID")
                )
        );

        assertTrue(
                arrangementService.findByLoadingPlanId(
                        plan.id()
                ).isEmpty()
        );

        arrangementService.save(
                plan.id(),
                requestSnapshot(3),
                previewSnapshot("VALID")
        );

        assertEquals(
                "VALID",
                arrangementService.findByLoadingPlanId(
                        plan.id()
                ).orElseThrow().status()
        );
    }

    @Test
    void arrangementReplacementWaitsForParentMovementLock() {

        LoadingPlan plan = createLoadingPlan();

        addCargo(plan, 3);

        arrangementService.save(
                plan.id(),
                requestSnapshot(3),
                previewSnapshot("VALID")
        );

        LoadingPlanArrangement original =
                arrangementService.findByLoadingPlanId(
                        plan.id()
                ).orElseThrow();

        assertBlockedByMovementLock(
                () -> arrangementService.save(
                        plan.id(),
                        requestSnapshot(3),
                        previewSnapshot("INVALID")
                )
        );

        assertEquals(
                original,
                arrangementService.findByLoadingPlanId(
                        plan.id()
                ).orElseThrow()
        );

        arrangementService.save(
                plan.id(),
                requestSnapshot(3),
                previewSnapshot("INVALID")
        );

        LoadingPlanArrangement updated =
                arrangementService.findByLoadingPlanId(
                        plan.id()
                ).orElseThrow();

        assertEquals("INVALID", updated.status());

        assertEquals(
                original.savedAt(),
                updated.savedAt()
        );
    }
}