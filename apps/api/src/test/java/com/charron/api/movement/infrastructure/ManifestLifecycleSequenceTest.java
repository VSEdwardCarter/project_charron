package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.ManifestLifecycleEventRepository;
import com.charron.api.movement.application.MovementManifestRepository;
import com.charron.api.movement.application.MovementRepository;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ManifestLifecycleSequenceTest {

    @Autowired
    private ManifestLifecycleEventRepository repository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    private Movement movement;
    private MovementManifest manifest;

    private final UUID actorId = UUID.randomUUID();

    private final Instant generatedAt =
            Instant.parse("2026-11-24T15:00:00Z");

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-SEQUENCE-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        manifest = createManifest(1);

        manifestRepository.save(manifest);
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
                generatedAt.plusSeconds(seconds),
                null
        );
    }

    private void assertRejected(
            ManifestLifecycleEvent event,
            String expectedMessage
    ) {

        DataIntegrityViolationException exception =
                assertThrows(
                        DataIntegrityViolationException.class,
                        () -> repository.save(event)
                );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(expectedMessage)
        );
    }

    @Test
    void rejectsIssuanceWithoutReview() {

        assertRejected(
                event(
                        ManifestStatus.REVIEWED,
                        ManifestStatus.ISSUED,
                        10
                ),
                "Invalid manifest lifecycle sequence"
        );
    }

    @Test
    void rejectsSupersessionWithoutIssuance() {

        repository.save(
                event(
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        10
                )
        );

        assertRejected(
                event(
                        ManifestStatus.ISSUED,
                        ManifestStatus.SUPERSEDED,
                        20
                ),
                "Invalid manifest lifecycle sequence"
        );
    }

    @Test
    void rejectsEventBeforeManifestGeneration() {

        assertRejected(
                event(
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        -10
                ),
                "Lifecycle event cannot precede manifest generation"
        );
    }

    @Test
    void rejectsEventBeforePreviousTransition() {

        repository.save(
                event(
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        20
                )
        );

        assertRejected(
                event(
                        ManifestStatus.REVIEWED,
                        ManifestStatus.ISSUED,
                        10
                ),
                "Lifecycle event cannot precede the previous transition"
        );
    }

    @Test
    void permitsCompleteValidLifecycle() {

        ManifestLifecycleEvent review = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );

        ManifestLifecycleEvent issue = event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                20
        );

        ManifestLifecycleEvent supersede = event(
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED,
                30
        );

        repository.save(review);
        repository.save(issue);
        repository.save(supersede);

        assertEquals(
                List.of(review, issue, supersede),
                repository.findByManifestId(manifest.id())
        );
    }

    @Test
    void rejectsTransitionAfterSupersession() {

        repository.save(
                event(
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        10
                )
        );

        repository.save(
                event(
                        ManifestStatus.REVIEWED,
                        ManifestStatus.ISSUED,
                        20
                )
        );

        repository.save(
                event(
                        ManifestStatus.ISSUED,
                        ManifestStatus.SUPERSEDED,
                        30
                )
        );

        assertRejected(
                event(
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        40
                ),
                "Invalid manifest lifecycle sequence"
        );
    }

    @Test
    void enforcesLifecycleIndependentlyForEachManifest() {

        MovementManifest second = createManifest(2);

        manifestRepository.save(second);

        ManifestLifecycleEvent firstReview = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );

        ManifestLifecycleEvent secondReview =
                new ManifestLifecycleEvent(
                        UUID.randomUUID(),
                        second.id(),
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        actorId,
                        generatedAt.plusSeconds(20),
                        null
                );

        repository.save(firstReview);
        repository.save(secondReview);

        assertEquals(
                List.of(firstReview),
                repository.findByManifestId(manifest.id())
        );

        assertEquals(
                List.of(secondReview),
                repository.findByManifestId(second.id())
        );
    }

    @Test
    void independentlyEnforcesEventPrimaryKeyUniqueness() {

        MovementManifest second = createManifest(2);

        manifestRepository.save(second);

        ManifestLifecycleEvent first = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10
        );

        repository.save(first);

        ManifestLifecycleEvent duplicateIdentity =
                new ManifestLifecycleEvent(
                        first.id(),
                        second.id(),
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        actorId,
                        generatedAt.plusSeconds(20),
                        null
                );

        assertThrows(
                DuplicateKeyException.class,
                () -> repository.save(duplicateIdentity)
        );
    }
}