package com.charron.api.movement.application;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.loading.application.LoadingCargoCatalogLookup;
import com.charron.api.loading.application.LoadingPlanArrangementRepository;
import com.charron.api.loading.application.LoadingPlanCargoRepository;
import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovementManifestPersistenceTest {

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
                "TEST-MANIFEST-PERSISTENCE",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        when(movementRepository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(movementRepository.findByIdForUpdate(movement.id()))
                .thenReturn(Optional.of(movement));

        when(assignmentRepository.findByMovementId(movement.id()))
                .thenReturn(List.of());

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of());
    }

    @Test
    void allocatesFirstManifestVersion() {

        when(manifestRepository.findLatestVersion(movement.id()))
                .thenReturn(0);

        MovementManifest manifest =
                service.generateAndPersist(movement.id());

        assertEquals(1, manifest.version());
        assertEquals(movement.id(), manifest.movementId());

        verify(manifestRepository).save(manifest);
    }

    @Test
    void incrementsExistingManifestVersion() {

        when(manifestRepository.findLatestVersion(movement.id()))
                .thenReturn(4);

        MovementManifest manifest =
                service.generateAndPersist(movement.id());

        assertEquals(5, manifest.version());

        verify(manifestRepository).save(manifest);
    }

    @Test
    void generatesDistinctManifestIdentities() {

        when(manifestRepository.findLatestVersion(movement.id()))
                .thenReturn(0, 1);

        MovementManifest first =
                service.generateAndPersist(movement.id());

        MovementManifest second =
                service.generateAndPersist(movement.id());

        assertNotEquals(first.id(), second.id());

        assertEquals(1, first.version());
        assertEquals(2, second.version());

        verify(manifestRepository).save(first);
        verify(manifestRepository).save(second);
    }

    @Test
    void acquiresMovementLockBeforeAllocatingVersion() {

        when(manifestRepository.findLatestVersion(movement.id()))
                .thenReturn(0);

        MovementManifest manifest =
                service.generateAndPersist(movement.id());

        InOrder order = inOrder(
                movementRepository,
                manifestRepository
        );

        order.verify(movementRepository)
                .findByIdForUpdate(movement.id());

        order.verify(manifestRepository)
                .findLatestVersion(movement.id());

        order.verify(manifestRepository)
                .save(manifest);
    }

    @Test
    void persistsCompleteMovementDetails() {

        MovementManifest manifest =
                service.generateAndPersist(movement.id());

        assertEquals(movement.id(), manifest.movementId());

        assertEquals(
                movement.reference(),
                manifest.movementReference()
        );

        assertEquals(movement.origin(), manifest.origin());
        assertEquals(movement.destination(), manifest.destination());

        assertEquals(
                movement.plannedDeparture(),
                manifest.plannedDeparture()
        );

        assertEquals(
                movement.plannedArrival(),
                manifest.plannedArrival()
        );

        assertNotNull(manifest.id());
        assertNotNull(manifest.generatedAt());

        verify(manifestRepository).save(manifest);
    }

    @Test
    void draftGenerationDoesNotPersistManifest() {

        MovementManifest draft =
                service.generateDraft(movement.id());

        assertNotNull(draft);

        verifyNoInteractions(manifestRepository);

        verify(movementRepository, never())
                .findByIdForUpdate(any(UUID.class));
    }

    @Test
    void rejectsPersistenceForUnknownMovement() {

        UUID unknownMovementId = UUID.randomUUID();

        when(movementRepository.findByIdForUpdate(unknownMovementId))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.generateAndPersist(unknownMovementId)
        );

        verifyNoInteractions(manifestRepository);
    }

    @Test
    void doesNotPersistWhenSnapshotGenerationFails() {

        LoadingPlan plan = LoadingPlan.create(
                movement.id(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of(plan));

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of());

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.generateAndPersist(movement.id())
        );

        verify(manifestRepository, never())
                .save(any(MovementManifest.class));
    }

    @Test
    void preventsManifestVersionOverflow() {

        when(manifestRepository.findLatestVersion(movement.id()))
                .thenReturn(Integer.MAX_VALUE);

        assertThrows(
                ArithmeticException.class,
                () -> service.generateAndPersist(movement.id())
        );

        verify(manifestRepository, never())
                .save(any(MovementManifest.class));
    }

    @Test
    void rejectsNullMovementIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> service.generateAndPersist(null)
        );

        verifyNoInteractions(manifestRepository);
        verifyNoInteractions(movementRepository);
    }
}