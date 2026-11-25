package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
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

class ManifestReviewServiceTest {

    private MovementManifestRepository manifestRepository;
    private ManifestLifecycleEventRepository eventRepository;
    private MovementRepository movementRepository;
    private MovementManifestService manifestService;
    private ManifestLifecycleService service;

    private Movement movement;
    private MovementManifest manifest;

    private final UUID actorId = UUID.randomUUID();

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
                "TEST-MANIFEST-REVIEW",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        manifest = createManifest(
                Instant.now().minusSeconds(60)
        );

        when(manifestRepository.findById(manifest.id()))
                .thenReturn(Optional.of(manifest));

        when(movementRepository.findByIdForUpdate(movement.id()))
                .thenReturn(Optional.of(movement));

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of());

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

    private MovementManifest createManifest(Instant generatedAt) {

        return new MovementManifest(
                UUID.randomUUID(),
                movement.id(),
                1,
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
            ManifestStatus previous,
            ManifestStatus next,
            long seconds
    ) {

        return new ManifestLifecycleEvent(
                UUID.randomUUID(),
                manifest.id(),
                previous,
                next,
                actorId,
                manifest.generatedAt().plusSeconds(seconds),
                null
        );
    }

    @Test
    void transitionsDraftManifestToReviewed() {

        ManifestLifecycle lifecycle = service.review(
                manifest.id(),
                actorId,
                "Manifest verified"
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                lifecycle.status()
        );

        assertEquals(manifest.id(), lifecycle.manifestId());
        assertEquals(1, lifecycle.events().size());
    }

    @Test
    void persistsCompleteReviewEvent() {

        service.review(
                manifest.id(),
                actorId,
                "Manifest verified"
        );

        ArgumentCaptor<ManifestLifecycleEvent> captor =
                ArgumentCaptor.forClass(
                        ManifestLifecycleEvent.class
                );

        verify(eventRepository).save(captor.capture());

        ManifestLifecycleEvent event = captor.getValue();

        assertNotNull(event.id());
        assertEquals(manifest.id(), event.manifestId());
        assertEquals(actorId, event.actorId());
        assertEquals(ManifestStatus.DRAFT, event.previousStatus());
        assertEquals(ManifestStatus.REVIEWED, event.newStatus());
        assertEquals("Manifest verified", event.remarks());

        assertFalse(
                event.recordedAt().isBefore(
                        manifest.generatedAt()
                )
        );
    }

    @Test
    void acquiresMovementLockBeforeReadingLifecycle() {

        service.review(manifest.id(), actorId, null);

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

        order.verify(eventRepository)
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void preservesOriginalManifestSnapshot() {

        service.review(manifest.id(), actorId, null);

        verify(manifestRepository, never())
                .save(any(MovementManifest.class));
    }

    @Test
    void permitsReviewWithoutRemarks() {

        ManifestLifecycle lifecycle =
                service.review(manifest.id(), actorId, null);

        assertNull(
                lifecycle.events().getFirst().remarks()
        );
    }

    @Test
    void rejectsRepeatedReview() {

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(
                        event(
                                ManifestStatus.DRAFT,
                                ManifestStatus.REVIEWED,
                                10
                        )
                ));

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.review(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void rejectsReviewOfIssuedManifest() {

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(
                        event(
                                ManifestStatus.DRAFT,
                                ManifestStatus.REVIEWED,
                                10
                        ),
                        event(
                                ManifestStatus.REVIEWED,
                                ManifestStatus.ISSUED,
                                20
                        )
                ));

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.review(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void rejectsReviewOfSupersededManifest() {

        when(eventRepository.findByManifestId(manifest.id()))
                .thenReturn(List.of(
                        event(
                                ManifestStatus.DRAFT,
                                ManifestStatus.REVIEWED,
                                10
                        ),
                        event(
                                ManifestStatus.REVIEWED,
                                ManifestStatus.ISSUED,
                                20
                        ),
                        event(
                                ManifestStatus.ISSUED,
                                ManifestStatus.SUPERSEDED,
                                30
                        )
                ));

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.review(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }

    @Test
    void rejectsUnknownManifest() {

        UUID unknownId = UUID.randomUUID();

        when(manifestRepository.findById(unknownId))
                .thenReturn(Optional.empty());

        assertThrows(
                ManifestNotFoundException.class,
                () -> service.review(
                        unknownId,
                        actorId,
                        null
                )
        );

        verifyNoInteractions(movementRepository);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void rejectsMissingParentMovement() {

        when(movementRepository.findByIdForUpdate(movement.id()))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.review(
                        manifest.id(),
                        actorId,
                        null
                )
        );

        verifyNoInteractions(eventRepository);
    }

    @Test
    void rejectsMissingActorIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> service.review(
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
    void rejectsMissingManifestIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> service.review(
                        null,
                        actorId,
                        null
                )
        );

        verifyNoInteractions(manifestRepository);
        verifyNoInteractions(movementRepository);
        verifyNoInteractions(eventRepository);
    }

    @Test
    void rejectsReviewBeforeManifestGeneration() {

        MovementManifest futureManifest =
                createManifest(
                        Instant.now().plusSeconds(3600)
                );

        when(manifestRepository.findById(futureManifest.id()))
                .thenReturn(Optional.of(futureManifest));

        when(eventRepository.findByManifestId(futureManifest.id()))
                .thenReturn(List.of());

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> service.review(
                        futureManifest.id(),
                        actorId,
                        null
                )
        );

        verify(eventRepository, never())
                .save(any(ManifestLifecycleEvent.class));
    }
}