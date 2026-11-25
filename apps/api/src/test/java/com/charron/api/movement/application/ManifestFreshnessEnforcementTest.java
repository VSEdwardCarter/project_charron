package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestChangeType;
import com.charron.api.movement.domain.ManifestFreshnessStatus;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ManifestFreshnessEnforcementTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private ManifestLifecycleEventRepository eventRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Movement movement;

    private final UUID reviewerId = UUID.randomUUID();
    private final UUID issuerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-FRESHNESS-ENFORCEMENT-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);
    }

    private MovementManifest generateManifest() {

        return manifestService.generateAndPersist(
                movement.id()
        );
    }

    private MovementManifest generateReviewedManifest() {

        MovementManifest manifest = generateManifest();

        lifecycleService.review(
                manifest.id(),
                reviewerId,
                "Reviewed"
        );

        return manifest;
    }

    private MovementManifest generateIssuedManifest() {

        MovementManifest manifest =
                generateReviewedManifest();

        lifecycleService.issue(
                manifest.id(),
                issuerId,
                "Issued"
        );

        return manifest;
    }

    private void changeDestination(String destination) {

        int updated = jdbcTemplate.update(
                """
                UPDATE movements
                SET destination = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                destination,
                movement.id()
        );

        assertEquals(1, updated);
    }

    private ManifestStatus status(UUID manifestId) {

        return lifecycleService
                .getLifecycle(manifestId)
                .status();
    }

    private ManifestFreshnessConflictException expectConflict(
            Runnable operation
    ) {

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_NESTED
        );

        return assertThrows(
                ManifestFreshnessConflictException.class,
                () -> transaction.executeWithoutResult(
                        ignored -> operation.run()
                )
        );
    }

    private void assertDestinationConflict(
            ManifestFreshnessConflictException exception
    ) {

        assertEquals(
                ManifestFreshnessStatus.STALE,
                exception.getAssessment().status()
        );

        assertEquals(
                Set.of(ManifestChangeType.DESTINATION),
                exception.getAssessment().changes()
        );
    }

    private void addPlanWithoutArrangement() {

        UUID equipmentId = UUID.randomUUID();
        UUID cargoSpaceId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id, name, category
                )
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "Test Trailer",
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
                "Test Cargo Space",
                "ENCLOSED",
                7000L,
                2400L,
                2500L
        );

        jdbcTemplate.update(
                """
                INSERT INTO loading_plans (
                    id,
                    movement_id,
                    equipment_id,
                    cargo_space_id,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?,
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                movement.id(),
                equipmentId,
                cargoSpaceId
        );
    }

    @Test
    void permitsReviewOfCurrentManifest() {

        MovementManifest manifest = generateManifest();

        lifecycleService.review(
                manifest.id(),
                reviewerId,
                null
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                status(manifest.id())
        );
    }

    @Test
    void permitsIssuanceOfCurrentManifest() {

        MovementManifest manifest =
                generateReviewedManifest();

        lifecycleService.issue(
                manifest.id(),
                issuerId,
                null
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(manifest.id())
        );
    }

    @Test
    void rejectsReviewOfStaleManifest() {

        MovementManifest manifest = generateManifest();

        changeDestination("Houston, TX");

        var exception = expectConflict(
                () -> lifecycleService.review(
                        manifest.id(),
                        reviewerId,
                        null
                )
        );

        assertDestinationConflict(exception);

        assertEquals(
                manifest.id(),
                exception.getAssessment().manifestId()
        );

        assertEquals(
                ManifestStatus.DRAFT,
                status(manifest.id())
        );

        assertTrue(
                eventRepository.findByManifestId(
                        manifest.id()
                ).isEmpty()
        );
    }

    @Test
    void rejectsIssuanceOfStaleManifest() {

        MovementManifest manifest =
                generateReviewedManifest();

        changeDestination("Houston, TX");

        var exception = expectConflict(
                () -> lifecycleService.issue(
                        manifest.id(),
                        issuerId,
                        null
                )
        );

        assertDestinationConflict(exception);

        assertEquals(
                ManifestStatus.REVIEWED,
                status(manifest.id())
        );

        assertEquals(
                1,
                eventRepository.findByManifestId(
                        manifest.id()
                ).size()
        );
    }

    @Test
    void rejectsReviewOfUnverifiableManifest() {

        MovementManifest manifest = generateManifest();

        addPlanWithoutArrangement();

        var exception = expectConflict(
                () -> lifecycleService.review(
                        manifest.id(),
                        reviewerId,
                        null
                )
        );

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                exception.getAssessment().status()
        );

        assertEquals(
                ManifestStatus.DRAFT,
                status(manifest.id())
        );

        assertTrue(
                eventRepository.findByManifestId(
                        manifest.id()
                ).isEmpty()
        );
    }

    @Test
    void rejectsIssuanceOfUnverifiableManifest() {

        MovementManifest manifest =
                generateReviewedManifest();

        addPlanWithoutArrangement();

        var exception = expectConflict(
                () -> lifecycleService.issue(
                        manifest.id(),
                        issuerId,
                        null
                )
        );

        assertEquals(
                ManifestFreshnessStatus.UNVERIFIABLE,
                exception.getAssessment().status()
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                status(manifest.id())
        );

        assertEquals(
                1,
                eventRepository.findByManifestId(
                        manifest.id()
                ).size()
        );
    }

    @Test
    void staleReplacementDoesNotSupersedeIssuedManifest() {

        MovementManifest original =
                generateIssuedManifest();

        MovementManifest replacement =
                generateReviewedManifest();

        changeDestination("Houston, TX");

        expectConflict(
                () -> lifecycleService.issue(
                        replacement.id(),
                        issuerId,
                        null
                )
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(original.id())
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                status(replacement.id())
        );

        assertEquals(
                2,
                eventRepository.findByManifestId(
                        original.id()
                ).size()
        );

        assertEquals(
                1,
                eventRepository.findByManifestId(
                        replacement.id()
                ).size()
        );
    }

    @Test
    void unverifiableReplacementDoesNotSupersedeIssuedManifest() {

        MovementManifest original =
                generateIssuedManifest();

        MovementManifest replacement =
                generateReviewedManifest();

        addPlanWithoutArrangement();

        expectConflict(
                () -> lifecycleService.issue(
                        replacement.id(),
                        issuerId,
                        null
                )
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(original.id())
        );

        assertEquals(
                ManifestStatus.REVIEWED,
                status(replacement.id())
        );

        assertEquals(
                2,
                eventRepository.findByManifestId(
                        original.id()
                ).size()
        );

        assertEquals(
                1,
                eventRepository.findByManifestId(
                        replacement.id()
                ).size()
        );
    }

    @Test
    void permitsIssuanceAfterOriginalValuesRestored() {

        MovementManifest manifest =
                generateReviewedManifest();

        changeDestination("Houston, TX");

        expectConflict(
                () -> lifecycleService.issue(
                        manifest.id(),
                        issuerId,
                        null
                )
        );

        changeDestination("Dallas, TX");

        lifecycleService.issue(
                manifest.id(),
                issuerId,
                null
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(manifest.id())
        );
    }

    @Test
    void permitsCurrentReplacementToSupersedeStaleIssuedManifest() {

        MovementManifest original =
                generateIssuedManifest();

        changeDestination("Houston, TX");

        assertTrue(
                manifestService.assessFreshness(
                        original.id()
                ).isStale()
        );

        MovementManifest replacement =
                generateReviewedManifest();

        lifecycleService.issue(
                replacement.id(),
                issuerId,
                null
        );

        assertEquals(
                ManifestStatus.SUPERSEDED,
                status(original.id())
        );

        assertEquals(
                ManifestStatus.ISSUED,
                status(replacement.id())
        );
    }

    @Test
    void rejectedTransitionPreservesHistoricalSnapshot() {

        MovementManifest manifest =
                generateReviewedManifest();

        changeDestination("Houston, TX");

        expectConflict(
                () -> lifecycleService.issue(
                        manifest.id(),
                        issuerId,
                        null
                )
        );

        assertEquals(
                manifest,
                manifestRepository.findById(
                        manifest.id()
                ).orElseThrow()
        );

        assertEquals(
                1,
                manifestRepository.findLatestVersion(
                        movement.id()
                )
        );
    }
}