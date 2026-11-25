package com.charron.api.movement.application;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.loading.application.LoadingCargoCatalogLookup;
import com.charron.api.loading.application.LoadingPlanArrangementRepository;
import com.charron.api.loading.application.LoadingPlanCargoRepository;
import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;
import com.charron.api.movement.domain.ManifestChangeType;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManifestResourceFreshnessTest {

    private MovementRepository movementRepository;
    private MovementManifestRepository manifestRepository;
    private MovementEquipmentAssignmentRepository assignmentRepository;
    private EquipmentRepository equipmentRepository;
    private LoadingPlanService loadingPlanService;
    private LoadingPlanCargoRepository cargoRepository;
    private LoadingCargoCatalogLookup cargoCatalog;
    private LoadingPlanArrangementRepository arrangementRepository;

    private MovementManifestService service;

    private Movement movement;
    private Equipment equipment;
    private MovementEquipmentAssignment assignment;
    private LoadingPlan plan;
    private LoadingPlanCargo cargoLine;
    private LoadingCargoCatalogLookup.CargoDetails cargoDetails;
    private LoadingPlanArrangement arrangement;
    private MovementManifest historical;

    private final UUID equipmentId = UUID.randomUUID();
    private final UUID cargoId = UUID.randomUUID();
    private final UUID cargoSpaceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movementRepository =
                mock(MovementRepository.class);

        manifestRepository =
                mock(MovementManifestRepository.class);

        assignmentRepository =
                mock(MovementEquipmentAssignmentRepository.class);

        equipmentRepository =
                mock(EquipmentRepository.class);

        loadingPlanService =
                mock(LoadingPlanService.class);

        cargoRepository =
                mock(LoadingPlanCargoRepository.class);

        cargoCatalog =
                mock(LoadingCargoCatalogLookup.class);

        arrangementRepository =
                mock(LoadingPlanArrangementRepository.class);

        service = new MovementManifestService(
                movementRepository,
                manifestRepository,
                assignmentRepository,
                equipmentRepository,
                loadingPlanService,
                cargoRepository,
                cargoCatalog,
                arrangementRepository
        );

        movement = Movement.plan(
                "TEST-RESOURCE-FRESHNESS",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        equipment = mock(Equipment.class);

        when(equipment.getId())
                .thenReturn(equipmentId);

        when(equipment.getName())
                .thenReturn("Test Equipment");

        when(equipment.getVin())
                .thenReturn("TEST-VIN");

        assignment = MovementEquipmentAssignment.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.values()[0]
        );

        plan = LoadingPlan.create(
                movement.id(),
                equipmentId,
                cargoSpaceId
        );

        cargoLine = LoadingPlanCargo.create(
                plan.id(),
                cargoId,
                10
        );

        cargoDetails =
                new LoadingCargoCatalogLookup.CargoDetails(
                        cargoId,
                        "Test Cargo",
                        1200L,
                        1000L,
                        800L,
                        50000L
                );

        arrangement = LoadingPlanArrangement.create(
                plan.id(),
                "{\"cargo\":[]}",
                "{\"placements\":[]}",
                "VALID"
        );

        configureBaseline();

        historical = service.generateDraft(
                movement.id()
        );

        when(manifestRepository.findById(historical.id()))
                .thenReturn(Optional.of(historical));
    }

    private void configureBaseline() {

        when(movementRepository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(assignmentRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(assignment));

        when(equipmentRepository.findIdentityById(equipmentId))
                .thenReturn(Optional.of(equipment));

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of(plan));

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(cargoLine));

        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.of(cargoDetails));

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(arrangement));
    }

    private ManifestFreshnessAssessment assess() {

        return service.assessFreshness(
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

    private void assertUnverifiable(
            String expectedReason
    ) {

        ManifestFreshnessAssessment assessment = assess();

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                assessment.status()
        );

        assertFalse(assessment.isCurrent());
        assertFalse(assessment.isVerifiable());
        assertTrue(assessment.changes().isEmpty());

        assertEquals(
                expectedReason,
                assessment.reason()
        );
    }

    @Test
    void unchangedResourcesProduceCurrentAssessment() {

        ManifestFreshnessAssessment assessment = assess();

        assertTrue(assessment.isCurrent());
        assertTrue(assessment.changes().isEmpty());

        assertEquals(1, historical.equipment().size());
        assertEquals(1, historical.cargo().size());
        assertEquals(1, historical.loadingPlans().size());
    }

    @Test
    void detectsEquipmentReplacement() {

        UUID replacementId = UUID.randomUUID();

        Equipment replacement = mock(Equipment.class);

        when(replacement.getId())
                .thenReturn(replacementId);

        when(replacement.getName())
                .thenReturn("Replacement Equipment");

        when(replacement.getVin())
                .thenReturn("REPLACEMENT-VIN");

        MovementEquipmentAssignment replacementAssignment =
                MovementEquipmentAssignment.assign(
                        movement.id(),
                        replacementId,
                        assignment.role()
                );

        when(assignmentRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(replacementAssignment));

        when(equipmentRepository.findIdentityById(replacementId))
                .thenReturn(Optional.of(replacement));

        assertStale(ManifestChangeType.EQUIPMENT);
    }

    @Test
    void detectsEquipmentRemoval() {

        when(assignmentRepository.findByMovementId(movement.id()))
                .thenReturn(List.of());

        assertStale(ManifestChangeType.EQUIPMENT);
    }

    @Test
    void detectsEquipmentNameChange() {

        when(equipment.getName())
                .thenReturn("Renamed Equipment");

        assertStale(ManifestChangeType.EQUIPMENT);
    }

    @Test
    void detectsEquipmentVinChange() {

        when(equipment.getVin())
                .thenReturn("UPDATED-VIN");

        assertStale(ManifestChangeType.EQUIPMENT);
    }

    @Test
    void returnsUnverifiableWhenAssignedEquipmentMissing() {

        when(equipmentRepository.findIdentityById(equipmentId))
                .thenReturn(Optional.empty());

        assertUnverifiable(
                "Assigned equipment not found: "
                        + equipmentId
        );
    }

    @Test
    void detectsCargoQuantityChange() {

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(
                        cargoLine.withQuantity(20)
                ));

        assertStale(ManifestChangeType.CARGO);
    }

    @Test
    void detectsCargoNameChange() {

        var changed =
                new LoadingCargoCatalogLookup.CargoDetails(
                        cargoId,
                        "Renamed Cargo",
                        cargoDetails.lengthMm(),
                        cargoDetails.widthMm(),
                        cargoDetails.heightMm(),
                        cargoDetails.weightGrams()
                );

        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.of(changed));

        assertStale(ManifestChangeType.CARGO);
    }

    @Test
    void detectsCargoDimensionChange() {

        var changed =
                new LoadingCargoCatalogLookup.CargoDetails(
                        cargoId,
                        cargoDetails.name(),
                        1500L,
                        cargoDetails.widthMm(),
                        cargoDetails.heightMm(),
                        cargoDetails.weightGrams()
                );

        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.of(changed));

        assertStale(ManifestChangeType.CARGO);
    }

    @Test
    void detectsCargoWeightChange() {

        var changed =
                new LoadingCargoCatalogLookup.CargoDetails(
                        cargoId,
                        cargoDetails.name(),
                        cargoDetails.lengthMm(),
                        cargoDetails.widthMm(),
                        cargoDetails.heightMm(),
                        75000L
                );

        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.of(changed));

        assertStale(ManifestChangeType.CARGO);
    }

    @Test
    void detectsCargoRemoval() {

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of());

        assertStale(ManifestChangeType.CARGO);
    }

    @Test
    void detectsCargoAddition() {

        UUID additionalCargoId = UUID.randomUUID();

        var additionalDetails =
                new LoadingCargoCatalogLookup.CargoDetails(
                        additionalCargoId,
                        "Additional Cargo",
                        500L,
                        500L,
                        500L,
                        10000L
                );

        LoadingPlanCargo additionalLine =
                LoadingPlanCargo.create(
                        plan.id(),
                        additionalCargoId,
                        5
                );

        when(cargoCatalog.findById(additionalCargoId))
                .thenReturn(Optional.of(additionalDetails));

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(
                        cargoLine,
                        additionalLine
                ));

        assertStale(ManifestChangeType.CARGO);
    }

    @Test
    void returnsUnverifiableWhenCargoCatalogEntryMissing() {

        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.empty());

        assertUnverifiable(
                "Cargo catalog entry not found: "
                        + cargoId
        );
    }

    @Test
    void detectsLoadingArrangementChange() {

        LoadingPlanArrangement changed =
                LoadingPlanArrangement.create(
                        plan.id(),
                        arrangement.requestSnapshot(),
                        "{\"placements\":[{\"x\":100}]}",
                        arrangement.status()
                );

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(changed));

        assertStale(ManifestChangeType.LOADING_PLANS);
    }

    @Test
    void detectsLoadingArrangementStatusChange() {

        LoadingPlanArrangement changed =
                LoadingPlanArrangement.create(
                        plan.id(),
                        arrangement.requestSnapshot(),
                        arrangement.previewSnapshot(),
                        "INVALID"
                );

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(changed));

        assertStale(ManifestChangeType.LOADING_PLANS);
    }

    @Test
    void detectsLoadingPlanRemoval() {

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of());

        assertStale(
                ManifestChangeType.CARGO,
                ManifestChangeType.LOADING_PLANS
        );
    }

    @Test
    void detectsLoadingPlanAddition() {

        LoadingPlan additionalPlan =
                LoadingPlan.create(
                        movement.id(),
                        equipmentId,
                        UUID.randomUUID()
                );

        LoadingPlanArrangement additionalArrangement =
                LoadingPlanArrangement.create(
                        additionalPlan.id(),
                        "{\"cargo\":[]}",
                        "{\"placements\":[]}",
                        "VALID"
                );

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of(
                        plan,
                        additionalPlan
                ));

        when(cargoRepository.findByLoadingPlanId(additionalPlan.id()))
                .thenReturn(List.of());

        when(arrangementRepository.findByLoadingPlanId(
                additionalPlan.id()
        )).thenReturn(Optional.of(additionalArrangement));

        assertStale(ManifestChangeType.LOADING_PLANS);
    }

    @Test
    void returnsUnverifiableWhenLoadingArrangementMissing() {

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.empty());

        assertUnverifiable(
                "No saved loading arrangement for plan: "
                        + plan.id()
        );
    }

    @Test
    void detectsMultipleResourceChanges() {

        when(equipment.getName())
                .thenReturn("Renamed Equipment");

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(
                        cargoLine.withQuantity(25)
                ));

        LoadingPlanArrangement changed =
                LoadingPlanArrangement.create(
                        plan.id(),
                        arrangement.requestSnapshot(),
                        "{\"placements\":[{\"x\":250}]}",
                        "INVALID"
                );

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(changed));

        assertStale(
                ManifestChangeType.EQUIPMENT,
                ManifestChangeType.CARGO,
                ManifestChangeType.LOADING_PLANS
        );
    }

    @Test
    void preservesHistoricalResourceSnapshot() {

        when(equipment.getName())
                .thenReturn("Renamed Equipment");

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(
                        cargoLine.withQuantity(50)
                ));

        assertStale(
                ManifestChangeType.EQUIPMENT,
                ManifestChangeType.CARGO
        );

        assertEquals(
                "Test Equipment",
                historical.equipment().getFirst().name()
        );

        assertEquals(
                10,
                historical.cargo().getFirst().quantity()
        );

        assertEquals(
                "{\"placements\":[]}",
                historical.loadingPlans()
                        .getFirst()
                        .arrangementSnapshot()
        );

        verify(manifestRepository, never())
                .save(any(MovementManifest.class));
    }

    @Test
    void returnsCurrentWhenOriginalResourceValuesRestored() {

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(
                        cargoLine.withQuantity(25)
                ));

        assertStale(ManifestChangeType.CARGO);

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(cargoLine));

        assertTrue(assess().isCurrent());
    }
}