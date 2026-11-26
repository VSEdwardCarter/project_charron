package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.MovementRepository;
import com.charron.api.movement.application.MovementManifestRepository;
import com.charron.api.movement.application.MovementQualityVerificationRepository;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.MovementQualityVerification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcMovementQualityVerificationRepositoryTest {

    @Autowired
    private MovementQualityVerificationRepository repository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestRepository manifestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Movement movement;

    private MovementManifest manifest;

    private final UUID reviewerId = UUID.randomUUID();

    private final Instant verifiedAt =
            Instant.parse("2026-11-25T20:00:00Z");

    @BeforeEach
    void setUp() {

        // Additional connection-level safety assertion.
        assertEquals(
                "charron_test",
                jdbcTemplate.queryForObject(
                        "SELECT current_database()",
                        String.class
                )
        );

        movement = Movement.plan(
                "TEST-QAQC-" + UUID.randomUUID(),
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z")
        );

        movementRepository.save(movement);

        manifest = createManifest(1);

        manifestRepository.save(manifest);
    }

    @Test
    void persistsAndRetrievesCompleteVerification() {

        var verification = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                "Inspection completed"
        );

        repository.save(verification);

        assertEquals(
                List.of(verification),
                repository.findByManifestId(manifest.id())
        );
    }

    @Test
    void preservesNullRemarks() {

        var verification = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                null
        );

        repository.save(verification);

        var restored = repository
                .findByManifestId(manifest.id())
                .getFirst();

        assertEquals(verification, restored);
        assertNull(restored.remarks());
    }

    @Test
    void returnsEmptyHistoryForUnverifiedManifest() {

        assertTrue(
                repository.findByManifestId(manifest.id())
                        .isEmpty()
        );
    }

    @Test
    void detectsExistingVerification() {

        assertFalse(
                repository.existsByManifestId(manifest.id())
        );

        repository.save(
                createVerification(
                        manifest.id(),
                        reviewerId,
                        verifiedAt,
                        null
                )
        );

        assertTrue(
                repository.existsByManifestId(manifest.id())
        );
    }

    @Test
    void retrievesHistoryInReverseChronologicalOrder() {

        var first = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                "Initial inspection"
        );

        var second = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt.plusSeconds(60),
                "Repeat inspection"
        );

        repository.save(first);
        repository.save(second);

        assertEquals(
                List.of(second, first),
                repository.findByManifestId(manifest.id())
        );
    }

    @Test
    void supportsMultipleReviewersForSameManifest() {

        var first = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                null
        );

        var second = createVerification(
                manifest.id(),
                UUID.randomUUID(),
                verifiedAt.plusSeconds(60),
                null
        );

        repository.save(first);
        repository.save(second);

        assertEquals(
                2,
                repository.findByManifestId(manifest.id()).size()
        );

        assertEquals(
                List.of(first),
                repository.findByReviewerId(reviewerId)
        );
    }

    @Test
    void retrievesReviewerHistoryAcrossManifestVersions() {

        MovementManifest secondManifest = createManifest(2);

        manifestRepository.save(secondManifest);

        var first = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                null
        );

        var second = createVerification(
                secondManifest.id(),
                reviewerId,
                verifiedAt.plusSeconds(60),
                null
        );

        repository.save(first);
        repository.save(second);

        assertEquals(
                List.of(second, first),
                repository.findByReviewerId(reviewerId)
        );

        assertEquals(
                List.of(first),
                repository.findByManifestId(manifest.id())
        );
    }

    @Test
    void rejectsUnknownManifest() {

        var verification = createVerification(
                UUID.randomUUID(),
                reviewerId,
                verifiedAt,
                null
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(verification)
        );
    }

    @Test
    void rejectsDuplicateVerificationId() {

        var verification = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                null
        );

        repository.save(verification);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> repository.save(verification)
        );
    }

    @Test
    void rejectsIncompleteChecklistThroughDirectSql() {

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO movement_quality_verifications (
                            id,
                            manifest_id,
                            reviewer_id,
                            equipment_assignment_verified,
                            equipment_inspection_completed,
                            cargo_reconciliation_verified,
                            loading_arrangement_verified,
                            cargo_securement_verified,
                            documentation_verified,
                            verified_at,
                            remarks
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        manifest.id(),
                        reviewerId,
                        true,
                        true,
                        true,
                        true,
                        false,
                        true,
                        Timestamp.from(verifiedAt),
                        null
                )
        );
    }

    @Test
    void rejectsDirectVerificationUpdate() {

        var verification = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                "Original inspection"
        );

        repository.save(verification);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        """
                        UPDATE movement_quality_verifications
                        SET remarks = ?
                        WHERE id = ?
                        """,
                        "Modified inspection",
                        verification.id()
                )
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Completed quality verification records are immutable"
                        )
        );
    }

    @Test
    void rejectsDirectVerificationDeletion() {

        var verification = createVerification(
                manifest.id(),
                reviewerId,
                verifiedAt,
                null
        );

        repository.save(verification);

        DataAccessException exception = assertThrows(
                DataAccessException.class,
                () -> jdbcTemplate.update(
                        """
                        DELETE FROM movement_quality_verifications
                        WHERE id = ?
                        """,
                        verification.id()
                )
        );

        assertTrue(
                exception.getMostSpecificCause()
                        .getMessage()
                        .contains(
                                "Completed quality verification records are immutable"
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
                verifiedAt.minusSeconds(3600)
        );
    }

    private MovementQualityVerification createVerification(
            UUID manifestId,
            UUID reviewerId,
            Instant timestamp,
            String remarks
    ) {

        return new MovementQualityVerification(
                UUID.randomUUID(),
                manifestId,
                reviewerId,
                true, true, true,
                true, true, true,
                timestamp,
                remarks
        );
    }
}