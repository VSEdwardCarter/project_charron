package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.MovementEquipmentRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DispatchManifestAssessmentIntegrationTest {

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private MovementManifestService manifestService;

    @Autowired
    private ManifestLifecycleService lifecycleService;

    @Autowired
    private DispatchAssessmentService assessmentService;

    @Autowired
    private MovementEquipmentAssignmentService assignmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Movement movement;

    private final UUID reviewerId = UUID.randomUUID();
    private final UUID issuerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movement = Movement.plan(
                "TEST-DISPATCH-MANIFEST-" + UUID.randomUUID(),
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
                "Dispatch assessment review"
        );

        return manifest;
    }

    private MovementManifest generateIssuedManifest() {

        MovementManifest manifest =
                generateReviewedManifest();

        lifecycleService.issue(
                manifest.id(),
                issuerId,
                "Dispatch assessment issuance"
        );

        return manifest;
    }

    private DispatchAssessment assess() {

        return assessmentService.assess(movement.id());
    }

    private List<String> manifestFindingCodes(
            DispatchAssessment assessment
    ) {

        return assessment.findings()
                .stream()
                .filter(finding ->
                        finding.code().startsWith("MANIFEST_")
                )
                .map(DispatchAssessment.Finding::code)
                .toList();
    }

    private void assertManifestBlocker(String code) {

        DispatchAssessment assessment = assess();

        assertEquals(
                List.of(code),
                manifestFindingCodes(assessment)
        );

        assertTrue(
                assessment.findings()
                        .stream()
                        .anyMatch(finding ->
                                finding.code().equals(code)
                                        && finding.severity()
                                        == DispatchAssessment.Severity.BLOCKER
                        )
        );
    }

    private void assertNoManifestBlockers() {

        assertTrue(
                manifestFindingCodes(assess()).isEmpty()
        );
    }

    private void changeDestination(String destination) {

        assertEquals(
                1,
                jdbcTemplate.update(
                        """
                        UPDATE movements
                        SET destination = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                        destination,
                        movement.id()
                )
        );
    }

    private UUID registerEquipment() {

        UUID equipmentId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment (
                    id, name, category
                )
                VALUES (?, ?, ?)
                """,
                equipmentId,
                "Dispatch Assessment Test Trailer",
                "TRAILER"
        );

        return equipmentId;
    }

    private void addLoadingPlanWithoutArrangement() {

        UUID equipmentId = registerEquipment();
        UUID cargoSpaceId = UUID.randomUUID();

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
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
                "Dispatch Assessment Cargo Space",
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
    void movementWithoutManifestIsBlocked() {

        assertManifestBlocker(
                "MANIFEST_NOT_ISSUED"
        );
    }

    @Test
    void draftManifestIsBlocked() {

        generateManifest();

        assertManifestBlocker(
                "MANIFEST_NOT_ISSUED"
        );
    }

    @Test
    void reviewedManifestIsBlocked() {

        generateReviewedManifest();

        assertManifestBlocker(
                "MANIFEST_NOT_ISSUED"
        );
    }

    @Test
    void currentIssuedManifestHasNoManifestBlockers() {

        generateIssuedManifest();

        assertNoManifestBlockers();
    }

    @Test
    void destinationChangeMakesIssuedManifestStale() {

        generateIssuedManifest();

        changeDestination("Houston, TX");

        assertManifestBlocker(
                "MANIFEST_STALE"
        );
    }

    @Test
    void equipmentAssignmentMakesIssuedManifestStale() {

        generateIssuedManifest();

        assignmentService.assign(
                movement.id(),
                registerEquipment(),
                MovementEquipmentRole.CARGO_CARRIER
        );

        assertManifestBlocker(
                "MANIFEST_STALE"
        );
    }

    @Test
    void missingArrangementMakesIssuedManifestUnverifiable() {

        generateIssuedManifest();

        addLoadingPlanWithoutArrangement();

        assertManifestBlocker(
                "MANIFEST_UNVERIFIABLE"
        );
    }

    @Test
    void newerDraftDoesNotInvalidateCurrentIssuedManifest() {

        generateIssuedManifest();

        generateManifest();

        assertNoManifestBlockers();
    }

    @Test
    void newerReviewedManifestDoesNotReplaceIssuedManifest() {

        generateIssuedManifest();

        generateReviewedManifest();

        assertNoManifestBlockers();
    }

    @Test
    void newerDraftDoesNotHideStaleIssuedManifest() {

        generateIssuedManifest();

        changeDestination("Houston, TX");

        generateManifest();

        assertManifestBlocker(
                "MANIFEST_STALE"
        );
    }

    @Test
    void currentReplacementClearsStaleManifestBlocker() {

        MovementManifest original =
                generateIssuedManifest();

        changeDestination("Houston, TX");

        assertManifestBlocker(
                "MANIFEST_STALE"
        );

        MovementManifest replacement =
                generateIssuedManifest();

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycleService.getLifecycle(
                        original.id()
                ).status()
        );

        assertEquals(
                ManifestStatus.ISSUED,
                lifecycleService.getLifecycle(
                        replacement.id()
                ).status()
        );

        assertNoManifestBlockers();
    }

    @Test
    void restoringOriginalValuesClearsManifestBlocker() {

        generateIssuedManifest();

        changeDestination("Houston, TX");

        assertManifestBlocker(
                "MANIFEST_STALE"
        );

        changeDestination("Dallas, TX");

        assertNoManifestBlockers();
    }

    @Test
    void currentIssuedManifestDoesNotOverrideOperationalBlockers() {

        generateIssuedManifest();

        DispatchAssessment assessment = assess();

        assertTrue(
                manifestFindingCodes(assessment).isEmpty()
        );

        assertTrue(
                assessment.hasBlockingFindings()
        );

        assertTrue(
                assessment.findings()
                        .stream()
                        .anyMatch(finding ->
                                finding.code().equals(
                                        "EQUIPMENT_NOT_ASSIGNED"
                                )
                        )
        );

        assertTrue(
                assessment.findings()
                        .stream()
                        .anyMatch(finding ->
                                finding.code().equals(
                                        "LOADING_PLAN_MISSING"
                                )
                        )
        );
    }
}