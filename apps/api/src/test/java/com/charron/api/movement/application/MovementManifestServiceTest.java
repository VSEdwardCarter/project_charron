
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
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovementManifestServiceTest {

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
    private UUID equipmentId;
    private UUID cargoId;
    private LoadingPlan plan;
    private LoadingPlanArrangement arrangement;

    @BeforeEach
    void setUp() {
        movementRepository = mock(MovementRepository.class);
        manifestRepository = mock(MovementManifestRepository.class);
        assignmentRepository =
                mock(MovementEquipmentAssignmentRepository.class);
        equipmentRepository = mock(EquipmentRepository.class);
        loadingPlanService = mock(LoadingPlanService.class);
        cargoRepository = mock(LoadingPlanCargoRepository.class);
        cargoCatalog = mock(LoadingCargoCatalogLookup.class);
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
                "MOV-MANIFEST-001",
                "Denver, CO",
                "Dallas, TX",
                java.time.Instant.parse("2026-12-01T08:00:00Z"),
                java.time.Instant.parse("2026-12-02T08:00:00Z")
        );

        equipmentId = UUID.randomUUID();
        cargoId = UUID.randomUUID();

        plan = LoadingPlan.create(
                movement.id(),
                equipmentId,
                UUID.randomUUID()
        );

        arrangement = LoadingPlanArrangement.create(
                plan.id(),
                "{\"cargoSpaceId\":\"" + plan.cargoSpaceId()
                        + "\",\"cargo\":[]}",
                "{\"status\":\"VALID\",\"placements\":[]}",
                "VALID"
        );

        Equipment equipment = mock(Equipment.class);

        when(equipment.getId()).thenReturn(equipmentId);
        when(equipment.getName()).thenReturn("Big Tex Trailer");
        when(equipment.getVin()).thenReturn("1TESTVIN123456789");

        when(movementRepository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(assignmentRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(
                        MovementEquipmentAssignment.assign(
                                movement.id(),
                                equipmentId,
                                MovementEquipmentRole.CARGO_CARRIER
                        )
                ));

        when(equipmentRepository.findIdentityById(equipmentId))
                .thenReturn(Optional.of(equipment));

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of(plan));

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(
                        LoadingPlanCargo.create(
                                plan.id(),
                                cargoId,
                                4
                        )
                ));

        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.of(
                        new LoadingCargoCatalogLookup.CargoDetails(
                                cargoId,
                                "Palletized Freight",
                                1200L,
                                1000L,
                                1500L,
                                250_000L
                        )
                ));

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(arrangement));
    }

    @Test
    void generatesDraftWithMovementDetails() {
        MovementManifest result =
                service.generateDraft(movement.id());

        assertEquals(movement.id(), result.movementId());
        assertEquals(movement.reference(), result.movementReference());
        assertEquals(movement.origin(), result.origin());
        assertEquals(movement.destination(), result.destination());
        assertEquals(movement.plannedDeparture(), result.plannedDeparture());
        assertEquals(movement.plannedArrival(), result.plannedArrival());
        assertEquals(1, result.version());
        assertNotNull(result.id());
        assertNotNull(result.generatedAt());
    }

    @Test
    void includesAssignedEquipment() {
        MovementManifest result =
                service.generateDraft(movement.id());

        assertEquals(1, result.equipment().size());

        var entry = result.equipment().getFirst();

        assertEquals(equipmentId, entry.equipmentId());
        assertEquals("Big Tex Trailer", entry.name());
        assertEquals(
                MovementEquipmentRole.CARGO_CARRIER,
                entry.role()
        );
        assertEquals("1TESTVIN123456789", entry.vin());
    }

    @Test
    void includesCargoDetailsAndQuantities() {
        MovementManifest result =
                service.generateDraft(movement.id());

        assertEquals(1, result.cargo().size());

        var entry = result.cargo().getFirst();

        assertEquals(plan.id(), entry.loadingPlanId());
        assertEquals(cargoId, entry.cargoId());
        assertEquals("Palletized Freight", entry.name());
        assertEquals(4, entry.quantity());
        assertEquals(1200L, entry.lengthMm());
        assertEquals(1000L, entry.widthMm());
        assertEquals(1500L, entry.heightMm());
        assertEquals(250_000L, entry.unitWeightGrams());
        assertEquals(1_000_000L, entry.totalWeightGrams());
    }

    @Test
    void includesSavedLoadingArrangement() {
        MovementManifest result =
                service.generateDraft(movement.id());

        assertEquals(1, result.loadingPlans().size());

        var entry = result.loadingPlans().getFirst();

        assertEquals(plan.id(), entry.loadingPlanId());
        assertEquals(equipmentId, entry.equipmentId());
        assertEquals(plan.cargoSpaceId(), entry.cargoSpaceId());
        assertEquals("VALID", entry.arrangementStatus());
        assertEquals(
                arrangement.previewSnapshot(),
                entry.arrangementSnapshot()
        );
    }

    @Test
    void rejectsMissingMovement() {
        when(movementRepository.findById(movement.id()))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.generateDraft(movement.id())
        );
    }

    @Test
    void rejectsMissingAssignedEquipment() {
        when(equipmentRepository.findIdentityById(equipmentId))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateDraft(movement.id())
        );

        assertTrue(
                exception.getMessage().contains(
                        "Assigned equipment not found"
                )
        );
    }

    @Test
    void rejectsMissingCargoCatalogEntry() {
        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateDraft(movement.id())
        );

        assertTrue(
                exception.getMessage().contains(
                        "Cargo catalog entry not found"
                )
        );
    }

    @Test
    void rejectsMissingLoadingArrangement() {
        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.generateDraft(movement.id())
        );

        assertTrue(
                exception.getMessage().contains(
                        "No saved loading arrangement"
                )
        );
    }

    @Test
    void supportsEmptyDraftWhenNoResourcesAssigned() {
        when(assignmentRepository.findByMovementId(movement.id()))
                .thenReturn(List.of());

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of());

        MovementManifest result =
                service.generateDraft(movement.id());

        assertTrue(result.equipment().isEmpty());
        assertTrue(result.cargo().isEmpty());
        assertTrue(result.loadingPlans().isEmpty());
    }

    @Test
    void preservesUnknownCargoWeight() {
        when(cargoCatalog.findById(cargoId))
                .thenReturn(Optional.of(
                        new LoadingCargoCatalogLookup.CargoDetails(
                                cargoId,
                                "Unweighed Freight",
                                null,
                                null,
                                null,
                                null
                        )
                ));

        MovementManifest result =
                service.generateDraft(movement.id());

        assertNull(result.cargo().getFirst().unitWeightGrams());
        assertNull(result.cargo().getFirst().totalWeightGrams());
    }
}
