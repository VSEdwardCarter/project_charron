package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestFreshnessStatus;


import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManifestIssuanceServiceTest {

    private MovementManifestRepository manifestRepository;
    private ManifestLifecycleEventRepository eventRepository;
    private MovementRepository movementRepository;
    private MovementManifestService manifestService;
    private ManifestLifecycleService service;

    private Movement movement;
    private MovementManifest manifest;

    private final UUID actorId = UUID.randomUUID();

    private final Instant generatedAt =
            Instant.now().minusSeconds(3600);

    @BeforeEach
    void setUp() {

        manifestRepository =
                mock(MovementManifestRepository.class);

        eventRepository =
                mock(ManifestLifecycleEventRepository.class);

        movementRepository =
                mock(MovementRepository.class);

        manifestService = mock(MovementManifestService.class);


        service = new ManifestLifecycleService(
                manifestRepository,
                eventRepository,
                movementRepository,
                manifestService
        );

        movement = Movement.plan(
                "TEST-MANIFEST-ISSUANCE",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        manifest = createManifest(2);

        when(manifestRepository.findById(manifest.id()))
                .thenReturn(Optional.of(manifest));

        when(movementRepository.findByIdForUpdate(movement.id()))
                .thenReturn(Optional.of(movement));

        when(manifestRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(manifest));

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(review(manifest)));

        when(manifestService.assessFreshness(any(UUID.class)))
                .thenAnswer(invocation ->
                        new ManifestFreshnessAssessment(
                                invocation.getArgument(0, UUID.class),
                                ManifestFreshnessStatus.CURRENT,
                                Set.of(),
                                Instant.now(),
                                null
                        )
                );
    }

    private MovementManifest createManifest(int version) {

        return new MovementManifest(
                UUID.randomUUID(),
                movement.id(),
                version,
                movement.reference(),
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival(),
                List.of(),
                List.of(),
                List.of(),
                generatedAt
        );
    }

    private ManifestLifecycleEvent event(
            MovementManifest target,
            ManifestStatus previous,
            ManifestStatus next,
            long seconds
    ) {

        return new ManifestLifecycleEvent(
                UUID.randomUUID(),
                target.id(),
                previous,
                next,
                actorId,
                generatedAt.plusSeconds(seconds),
                null
        );
    }

    private ManifestLifecycleEvent review(
            MovementManifest target
    ) {

        return event(
                target,
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );
    }

    private ManifestLifecycleEvent issuance(
            MovementManifest target
    ) {

        return event(
                target,
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                20
        );
    }

    private ManifestLifecycleEvent supersession(
            MovementManifest target
    ) {

        return event(
                target,
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED,
                30
        );
    }

    private void configureIssuedManifest(
            MovementManifest target
    ) {

        when(eventRepository.findByManifestId(target.id()))
                .thenReturn(List.of(
                        review(target),
                        issuance(target)
                ));
    }

    @Test
    void issuesReviewedManifest() {

        ManifestLifecycle result = service.issue(
                manifest.id(),
                actorId,
                "Approved for issuance"
        );

        assertEquals(
                ManifestStatus.ISSUED,
                result.status()
        );

        assertTrue(result.isIssued());
        assertEquals(2, result.events().size());

        verify(eventRepository)
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void persistsCompleteIssuanceEvent() {

        service.issue(
                manifest.id(),
                actorId,
                "Approved"
        );

        ArgumentCaptor<ManifestLifecycleEvent> captor =
                ArgumentCaptor.forClass(
                        ManifestLifecycleEvent.class
                );

        verify(eventRepository).save(captor.capture());

        ManifestLifecycleEvent saved = captor.getValue();

        assertNotNull(saved.id());
        assertEquals(manifest.id(), saved.manifestId());
        assertEquals(actorId, saved.actorId());

        assertEquals(
                ManifestStatus.REVIEWED,
                saved.previousStatus()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                saved.newStatus()
        );

        assertEquals("Approved", saved.remarks());

        assertFalse(
                saved.recordedAt().isBefore(
                        manifest.generatedAt()
                )
        );
    }

    @Test
    void rejectsIssuanceOfDraftManifest() {

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of());

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.issue(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void rejectsRepeatedIssuance() {

        configureIssuedManifest(manifest);

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.issue(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void rejectsIssuanceOfSupersededManifest() {

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(
                        review(manifest),
                        issuance(manifest),
                        supersession(manifest)
                ));

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.issue(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void supersedesPreviouslyIssuedManifest() {

        MovementManifest previous = createManifest(1);

        configureIssuedManifest(previous);

        when(manifestRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(manifest, previous));

        ManifestLifecycle result = service.issue(
                manifest.id(),
                actorId,
                "Replacement issued"
        );

        assertEquals(
                ManifestStatus.ISSUED,
                result.status()
        );

        ArgumentCaptor<ManifestLifecycleEvent> captor =
                ArgumentCaptor.forClass(
                        ManifestLifecycleEvent.class
                );

        verify(eventRepository, times(2))
                .save(captor.capture());

        List<ManifestLifecycleEvent> saved =
                captor.getAllValues();

        ManifestLifecycleEvent superseded = saved.getFirst();
        ManifestLifecycleEvent issued = saved.getLast();

        assertEquals(
                previous.id(),
                superseded.manifestId()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                superseded.previousStatus()
        );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                superseded.newStatus()
        );

        assertEquals(
                manifest.id(),
                issued.manifestId()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                issued.newStatus()
        );

        assertEquals(
                superseded.recordedAt(),
                issued.recordedAt()
        );

        assertEquals(actorId, superseded.actorId());

        assertTrue(
                superseded.remarks().contains(
                        manifest.id().toString()
                )
        );
    }

    @Test
    void rejectsOlderManifestAfterNewerVersionIssued() {

        MovementManifest newer = createManifest(3);

        configureIssuedManifest(newer);

        when(manifestRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(newer, manifest));

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.issue(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void rejectsOlderManifestAfterNewerVersionSuperseded() {

        MovementManifest newer = createManifest(3);

        when(eventRepository.findByManifestId(newer.id()))
                .thenReturn(List.of(
                        review(newer),
                        issuance(newer),
                        supersession(newer)
                ));

        when(manifestRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(newer, manifest));

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.issue(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void permitsIssuanceWhenNewerDraftExists() {

        MovementManifest newer = createManifest(3);

        when(eventRepository.findByManifestId(newer.id()))
                .thenReturn(List.of());

        when(manifestRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(newer, manifest));

        ManifestLifecycle result = service.issue(
                manifest.id(),
                actorId,
                null
        );

        assertEquals(
                ManifestStatus.ISSUED,
                result.status()
        );

        verify(eventRepository, times(1))
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void rejectsMultipleActiveIssuedManifests() {

        MovementManifest previous = createManifest(1);
        MovementManifest another = createManifest(1);

        configureIssuedManifest(previous);
        configureIssuedManifest(another);

        when(manifestRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(
                        manifest,
                        previous,
                        another
                ));

        ManifestLifecycleConflictException exception =
                assertThrows(
                        ManifestLifecycleConflictException.class,
                        () -> service.issue(
                                manifest.id(),
                                actorId,
                                null
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "Multiple active issued manifests"
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void acquiresMovementLockBeforeInspectingHistory() {

        service.issue(manifest.id(), actorId, null);

        InOrder order = inOrder(
                manifestRepository,
                movementRepository,
                eventRepository
        );

        order.verify(manifestRepository)
                .findById(manifest.id());

        order.verify(movementRepository)
                .findByIdForUpdate(movement.id());

        order.verify(eventRepository)
                .findByManifestId(manifest.id());

        order.verify(manifestRepository)
                .findByMovementId(movement.id());

        order.verify(eventRepository)
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void preservesImmutableManifestSnapshots() {

        MovementManifest previous = createManifest(1);

        configureIssuedManifest(previous);

        when(manifestRepository.findByMovementId(movement.id()))
                .thenReturn(List.of(manifest, previous));

        service.issue(manifest.id(), actorId, null);

        verify(manifestRepository, never())
                .save(any(MovementManifest.class));
    }

    @Test
    void rejectsUnknownManifest() {

        UUID unknownId = UUID.randomUUID();

        when(manifestRepository.findById(unknownId))
                .thenReturn(Optional.empty());

        assertThrows(
                ManifestNotFoundException.class,
                () -> service.issue(
                        unknownId,
                        actorId,
                        null
                )
        );

        verifyNoInteractions(movementRepository);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void rejectsMissingActorIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> service.issue(
                        manifest.id(),
                        null,
                        null
                )
        );

        verifyNoInteractions(manifestRepository);
        verifyNoInteractions(movementRepository);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void rejectsMissingParentMovement() {

        when(movementRepository.findByIdForUpdate(movement.id()))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.issue(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verifyNoInteractions(eventRepository);
    }
}