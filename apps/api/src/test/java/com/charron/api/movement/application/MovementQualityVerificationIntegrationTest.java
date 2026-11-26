package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.MovementQualityVerification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MovementQualityVerificationIntegrationTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private MovementQualityVerificationService verificationService;

    @Autowired
    private MovementQualityVerificationRepository verificationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Movement movement;

    private MovementManifest manifest;

    private final UUID reviewerId = UUID.randomUUID();

    private final UUID manifestActorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        assertEquals(
                "charron_test",
                jdbcTemplate.queryForObject(
                        "SELECT current_database()",
                        String.class
                )
        );

        movement = Movement.plan(
                "TEST-QAQC-INT-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        manifest = manifestService.generateAndPersist(
                movement.id()
        );
    }

    @Test
    void persistsCompletedVerificationForIssuedManifest() {

        issueManifest(manifest);

        MovementQualityVerification verification =
                completeVerification(manifest);

        var history = verificationService.getHistory(
                movement.id(),
                manifest.id()
        );

        assertEquals(1, history.size());

        assertEquals(
                verification,
                history.getFirst()
        );

        assertEquals(
                reviewerId,
                history.getFirst().reviewerId()
        );

        assertTrue(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );
    }

    @Test
    void rejectsDraftManifestWithoutPersistingVerification() {

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> completeVerification(manifest)
        );

        assertFalse(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );
    }

    @Test
    void rejectsReviewedButUnissuedManifest() {

        lifecycleService.review(
                manifest.id(),
                manifestActorId,
                "Reviewed for integration test"
        );

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> completeVerification(manifest)
        );

        assertFalse(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );
    }

    @Test
    void rejectsStaleIssuedManifest() {

        issueManifest(manifest);

        jdbcTemplate.update(
                """
                UPDATE movements
                SET destination = ?
                WHERE id = ?
                """,
                "Houston, TX",
                movement.id()
        );

        assertFalse(
                manifestService.assessFreshness(
                        manifest.id()
                ).isCurrent()
        );

        assertThrows(
                ManifestFreshnessConflictException.class,
                () -> completeVerification(manifest)
        );

        assertFalse(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );
    }

    @Test
    void preservesVerificationAfterManifestSupersession() {

        issueManifest(manifest);

        var originalVerification =
                completeVerification(manifest);

        var replacement =
                manifestService.generateAndPersist(
                        movement.id()
                );

        issueManifest(replacement);

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycleService.getLifecycle(
                        manifest.id()
                ).status()
        );

        var history = verificationService.getHistory(
                movement.id(),
                manifest.id()
        );

        assertEquals(1, history.size());

        assertEquals(
                originalVerification,
                history.getFirst()
        );

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> completeVerification(manifest)
        );
    }

    @Test
    void replacementManifestRequiresIndependentVerification() {

        issueManifest(manifest);

        completeVerification(manifest);

        var replacement =
                manifestService.generateAndPersist(
                        movement.id()
                );

        issueManifest(replacement);

        assertTrue(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );

        assertFalse(
                verificationRepository.existsByManifestId(
                        replacement.id()
                )
        );

        completeVerification(replacement);

        assertTrue(
                verificationRepository.existsByManifestId(
                        replacement.id()
                )
        );
    }

    @Test
    void rejectsIncompleteVerificationWithoutPersistence() {

        issueManifest(manifest);

        assertThrows(
                IllegalArgumentException.class,
                () -> verificationService.complete(
                        movement.id(),
                        manifest.id(),
                        reviewerId,
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        "Incomplete inspection"
                )
        );

        assertFalse(
                verificationRepository.existsByManifestId(
                        manifest.id()
                )
        );
    }

    private void issueManifest(MovementManifest target) {

        lifecycleService.review(
                target.id(),
                manifestActorId,
                "Integration test review"
        );

        lifecycleService.issue(
                target.id(),
                manifestActorId,
                "Integration test issuance"
        );
    }

    private MovementQualityVerification completeVerification(
            MovementManifest target
    ) {

        return verificationService.complete(
                movement.id(),
                target.id(),
                reviewerId,
                true, true, true,
                true, true, true,
                "Integration test verification"
        );
    }
}