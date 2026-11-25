package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.ManifestLifecycleEventRepository;
import com.charron.api.movement.application.MovementManifestRepository;
import com.charron.api.movement.application.MovementRepository;
import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcManifestLifecycleEventRepositoryTest {

    @Autowired
    private ManifestLifecycleEventRepository repository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MovementManifest manifest;

    private final UUID actorId = UUID.randomUUID();

    private final Instant generatedAt =
            Instant.parse("2026-11-24T15:00:00Z");

    @BeforeEach
    void setUp() {

        Movement movement = Movement.plan(
                "TEST-LIFECYCLE-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        manifest = new MovementManifest(
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

        manifestRepository.save(manifest);
    }

    private ManifestLifecycleEvent event(
            ManifestStatus previous,
            ManifestStatus next,
            long seconds,
            String remarks
    ) {

        return new ManifestLifecycleEvent(
                UUID.randomUUID(),
                manifest.id(),
                previous,
                next,
                actorId,
                generatedAt.plusSeconds(seconds),
                remarks
        );
    }

    private ManifestLifecycleEvent review() {

        return event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10,
                "Manifest reviewed"
        );
    }

    private ManifestLifecycleEvent issue() {

        return event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                20,
                "Manifest issued"
        );
    }

    private ManifestLifecycleEvent supersede() {

        return event(
                ManifestStatus.ISSUED,
                ManifestStatus.SUPERSEDED,
                30,
                "Replaced by newer manifest"
        );
    }

    @Test
    void insertsAndRetrievesCompleteEvent() {

        ManifestLifecycleEvent event = review();

        repository.save(event);

        ManifestLifecycleEvent restored =
                repository.findById(event.id())
                        .orElseThrow();

        assertEquals(event, restored);
    }

    @Test
    void preservesNullRemarks() {

        ManifestLifecycleEvent event = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10,
                null
        );

        repository.save(event);

        assertEquals(
                event,
                repository.findById(event.id())
                        .orElseThrow()
        );

        assertNull(
                repository.findById(event.id())
                        .orElseThrow()
                        .remarks()
        );
    }

    @Test
    void retrievesCompleteLifecycleHistory() {

        ManifestLifecycleEvent review = review();
        ManifestLifecycleEvent issue = issue();
        ManifestLifecycleEvent supersede = supersede();

        repository.save(review);
        repository.save(issue);
        repository.save(supersede);

        assertEquals(
                List.of(review, issue, supersede),
                repository.findByManifestId(manifest.id())
        );
    }

    @Test
    void ordersEventsWithIdenticalTimestamps() {

        ManifestLifecycleEvent review = event(
                ManifestStatus.DRAFT,
                ManifestStatus.REVIEWED,
                10,
                null
        );

        ManifestLifecycleEvent issue = event(
                ManifestStatus.REVIEWED,
                ManifestStatus.ISSUED,
                10,
                null
        );

        repository.save(review);
        repository.save(issue);

        assertEquals(
                List.of(review, issue),
                repository.findByManifestId(manifest.id())
        );
    }

    @Test
    void returnsEmptyForUnknownEvent() {

        assertTrue(
                repository.findById(
                        UUID.randomUUID()
                ).isEmpty()
        );
    }

    @Test
    void returnsEmptyHistoryForDraftManifest() {

        assertTrue(
                repository.findByManifestId(
                        manifest.id()
                ).isEmpty()
        );
    }

    @Test
    void rejectsDuplicateEventIdentity() {

        ManifestLifecycleEvent event = review();

        repository.save(event);

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(event)
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains("Invalid manifest lifecycle sequence")
        );
    }

    @Test
    void rejectsDuplicateLifecycleTransition() {

        repository.save(review());

        DataIntegrityViolationException exception = assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(review())
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains("Invalid manifest lifecycle sequence")
        );
    }

    @Test
    void rejectsEventForUnknownManifest() {

        ManifestLifecycleEvent event =
                new ManifestLifecycleEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        ManifestStatus.DRAFT,
                        ManifestStatus.REVIEWED,
                        actorId,
                        generatedAt.plusSeconds(10),
                        null
                );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(event)
        );
    }

    @Test
    void rejectsInvalidTransitionThroughDirectSql() {

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update("""
                        INSERT INTO manifest_lifecycle_events (
                            id,
                            manifest_id,
                            previous_status,
                            new_status,
                            actor_id,
                            recorded_at,
                            remarks
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        manifest.id(),
                        "DRAFT",
                        "ISSUED",
                        actorId,
                        java.sql.Timestamp.from(
                                generatedAt.plusSeconds(10)
                        ),
                        null
                )
        );
    }

    @Test
    void rejectsDirectEventUpdate() {

        ManifestLifecycleEvent event = review();

        repository.save(event);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update("""
                        UPDATE manifest_lifecycle_events
                        SET remarks = ?
                        WHERE id = ?
                        """,
                        "Modified remarks",
                        event.id()
                )
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Manifest lifecycle events are immutable"
                        )
        );
    }

    @Test
    void rejectsDirectEventDeletion() {

        ManifestLifecycleEvent event = review();

        repository.save(event);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update("""
                        DELETE FROM manifest_lifecycle_events
                        WHERE id = ?
                        """,
                        event.id()
                )
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Manifest lifecycle events are immutable"
                        )
        );
    }

    @Test
    void reconstructsLifecycleFromPersistedHistory() {

        repository.save(review());
        repository.save(issue());
        repository.save(supersede());

        ManifestLifecycle lifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        repository.findByManifestId(
                                manifest.id()
                        )
                );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycle.status()
        );

        assertEquals(3, lifecycle.events().size());
        assertTrue(lifecycle.isTerminal());
    }
}