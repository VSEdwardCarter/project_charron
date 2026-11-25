package com.charron.api.movement.application;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.loading.application.LoadingCargoCatalogLookup;
import com.charron.api.loading.application.LoadingPlanArrangementRepository;
import com.charron.api.loading.application.LoadingPlanCargoRepository;
import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.movement.domain.ManifestChangeType;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManifestFreshnessServiceTest {

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
    private MovementManifest manifest;

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
                "TEST-MANIFEST-FRESHNESS",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        manifest = createManifest(
                movement.reference(),
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival()
        );

        when(manifestRepository.findById(manifest.id()))
                .thenReturn(Optional.of(manifest));

        when(movementRepository.findById(movement.id()))
                .thenReturn(Optional.of(movement));

        when(assignmentRepository.findByMovementId(movement.id()))
                .thenReturn(List.of());

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenReturn(List.of());
    }

    private MovementManifest createManifest(
            String reference,
            String origin,
            String destination,
            Instant departure,
            Instant arrival
    ) {

        return new MovementManifest(
                UUID.randomUUID(),
                movement.id(),
                3,
                reference,
                origin,
                destination,
                departure,
                arrival,
                List.of(),
                List.of(),
                List.of(),
                Instant.now().minusSeconds(60)
        );
    }

    private void configurePersistedManifest(
            MovementManifest persisted
    ) {

        when(manifestRepository.findById(persisted.id()))
                .thenReturn(Optional.of(persisted));
    }

    @Test
    void returnsCurrentForUnchangedMovement() {

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(manifest.id());

        assertEquals(
                ManifestFreshnessStatus.CURRENT,
                assessment.status()
        );

        assertEquals(
                manifest.id(),
                assessment.manifestId()
        );

        assertTrue(assessment.isCurrent());
        assertTrue(assessment.changes().isEmpty());
        assertNull(assessment.reason());
        assertNotNull(assessment.assessedAt());
    }

    @Test
    void detectsChangedMovementReference() {

        MovementManifest historical = createManifest(
                "PREVIOUS-REFERENCE",
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival()
        );

        configurePersistedManifest(historical);

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(historical.id());

        assertEquals(
                ManifestFreshnessStatus.STALE,
                assessment.status()
        );

        assertEquals(
                Set.of(ManifestChangeType.MOVEMENT_REFERENCE),
                assessment.changes()
        );
    }

    @Test
    void detectsChangedOrigin() {

        MovementManifest historical = createManifest(
                movement.reference(),
                "Colorado Springs, CO",
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival()
        );

        configurePersistedManifest(historical);

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(historical.id());

        assertTrue(assessment.isStale());

        assertEquals(
                Set.of(ManifestChangeType.ORIGIN),
                assessment.changes()
        );
    }

    @Test
    void detectsChangedDestination() {

        MovementManifest historical = createManifest(
                movement.reference(),
                movement.origin(),
                "Houston, TX",
                movement.plannedDeparture(),
                movement.plannedArrival()
        );

        configurePersistedManifest(historical);

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(historical.id());

        assertTrue(assessment.isStale());

        assertEquals(
                Set.of(ManifestChangeType.DESTINATION),
                assessment.changes()
        );
    }

    @Test
    void detectsChangedDeparture() {

        MovementManifest historical = createManifest(
                movement.reference(),
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture().minusSeconds(3600),
                movement.plannedArrival()
        );

        configurePersistedManifest(historical);

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(historical.id());

        assertTrue(assessment.isStale());

        assertEquals(
                Set.of(ManifestChangeType.PLANNED_DEPARTURE),
                assessment.changes()
        );
    }

    @Test
    void detectsChangedArrival() {

        MovementManifest historical = createManifest(
                movement.reference(),
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival().plusSeconds(3600)
        );

        configurePersistedManifest(historical);

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(historical.id());

        assertTrue(assessment.isStale());

        assertEquals(
                Set.of(ManifestChangeType.PLANNED_ARRIVAL),
                assessment.changes()
        );
    }

    @Test
    void detectsMultipleMovementChanges() {

        MovementManifest historical = createManifest(
                "PREVIOUS-REFERENCE",
                "Colorado Springs, CO",
                "Houston, TX",
                movement.plannedDeparture().minusSeconds(3600),
                movement.plannedArrival().plusSeconds(3600)
        );

        configurePersistedManifest(historical);

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(historical.id());

        assertTrue(assessment.isStale());

        assertEquals(
                Set.of(
                        ManifestChangeType.MOVEMENT_REFERENCE,
                        ManifestChangeType.ORIGIN,
                        ManifestChangeType.DESTINATION,
                        ManifestChangeType.PLANNED_DEPARTURE,
                        ManifestChangeType.PLANNED_ARRIVAL
                ),
                assessment.changes()
        );
    }

    @Test
    void returnsUnverifiableWhenMovementUnavailable() {

        when(movementRepository.findById(movement.id()))
                .thenReturn(Optional.empty());

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(manifest.id());

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                assessment.status()
        );

        assertFalse(assessment.isVerifiable());
        assertFalse(assessment.isCurrent());
        assertTrue(assessment.changes().isEmpty());

        assertEquals(
                "Associated movement could not be found",
                assessment.reason()
        );

        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(loadingPlanService);
    }

    @Test
    void returnsUnverifiableForUnavailableSnapshot() {

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenThrow(
                        new ManifestSnapshotUnavailableException(
                                "Loading arrangement unavailable"
                        )
                );

        ManifestFreshnessAssessment assessment =
                service.assessFreshness(manifest.id());

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                assessment.status()
        );

        assertEquals(
                "Loading arrangement unavailable",
                assessment.reason()
        );

        assertTrue(assessment.changes().isEmpty());
    }

    @Test
    void propagatesUnexpectedDatabaseFailure() {

        when(assignmentRepository.findByMovementId(movement.id()))
                .thenThrow(
                        new DataAccessResourceFailureException(
                                "Database unavailable"
                        )
                );

        assertThrows(
                DataAccessResourceFailureException.class,
                () -> service.assessFreshness(manifest.id())
        );
    }

    @Test
    void propagatesUnexpectedProgrammingFailure() {

        when(loadingPlanService.getByMovementId(movement.id()))
                .thenThrow(
                        new IllegalStateException(
                                "Unexpected internal failure"
                        )
                );

        assertThrows(
                IllegalStateException.class,
                () -> service.assessFreshness(manifest.id())
        );
    }

    @Test
    void rejectsUnknownManifest() {

        UUID unknownId = UUID.randomUUID();

        when(manifestRepository.findById(unknownId))
                .thenReturn(Optional.empty());

        assertThrows(
                ManifestNotFoundException.class,
                () -> service.assessFreshness(unknownId)
        );

        verifyNoInteractions(movementRepository);
        verifyNoInteractions(assignmentRepository);
        verifyNoInteractions(loadingPlanService);
    }

    @Test
    void rejectsNullManifestIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> service.assessFreshness(null)
        );

        verifyNoInteractions(manifestRepository);
        verifyNoInteractions(movementRepository);
    }

    @Test
    void doesNotPersistAnotherManifest() {

        service.assessFreshness(manifest.id());

        verify(manifestRepository, never())
                .save(any(MovementManifest.class));
    }

    @Test
    void doesNotAllocateAnotherManifestVersion() {

        service.assessFreshness(manifest.id());

        verify(manifestRepository, never())
                .findLatestVersion(any(UUID.class));
    }

    @Test
    void doesNotAcquireMovementWriteLock() {

        service.assessFreshness(manifest.id());

        verify(movementRepository)
                .findById(movement.id());

        verify(movementRepository, never())
                .findByIdForUpdate(any(UUID.class));
    }

    @Test
    void reconstructsCurrentOperationalInformation() {

        service.assessFreshness(manifest.id());

        verify(assignmentRepository)
                .findByMovementId(movement.id());

        verify(loadingPlanService)
                .getByMovementId(movement.id());
    }

    @Test
    void reassessesCurrentInformationOnEveryRequest() {

        ManifestFreshnessAssessment first =
                service.assessFreshness(manifest.id());

        assertTrue(first.isCurrent());

        when(movementRepository.findById(movement.id()))
                .thenReturn(Optional.empty());

        ManifestFreshnessAssessment second =
                service.assessFreshness(manifest.id());

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                second.status()
        );

        verify(movementRepository, times(2))
                .findById(movement.id());
    }
}