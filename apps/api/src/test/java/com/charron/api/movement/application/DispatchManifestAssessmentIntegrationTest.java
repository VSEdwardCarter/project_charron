package com.charron.api.movement.application;

import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.MovementEquipmentRole;
import com.charron.api.movement.application
        .MovementQualityVerificationService.VerificationStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

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
    private MovementQualityVerificationService verificationService;

    @Autowired
    private MovementEquipmentAssignmentService assignmentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Movement movement;

    private MovementQualityVerificationService.VerificationStatus verificationStatus(
            MovementManifest manifest
    ) {

        return verificationService.getVerificationStatus(
                movement.id(),
                manifest.id()
        );
    }
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

    private void completeQualityVerification(
            MovementManifest manifest
    ) {

        verificationService.complete(
                movement.id(),
                manifest.id(),
                reviewerId,
                true, true, true,
                true, true, true,
                "Dispatch QA/QC integration test"
        );
    }

    private boolean hasQualityBlocker() {

        return assess().findings()
                .stream()
                .anyMatch(finding ->
                        finding.code().equals(
                                "QUALITY_VERIFICATION_REQUIRED"
                        )
                        && finding.severity()
                        == DispatchAssessment.Severity.BLOCKER
                );
    }

    @Test
    void currentIssuedManifestRequiresQualityVerification() {

        generateIssuedManifest();

        assertNoManifestBlockers();
        assertTrue(hasQualityBlocker());
    }

    @Test
    void completedQualityVerificationClearsQualityBlocker() {

        MovementManifest manifest = generateIssuedManifest();

        assertTrue(hasQualityBlocker());

        completeQualityVerification(manifest);

        assertFalse(hasQualityBlocker());
        assertNoManifestBlockers();
    }

    @Test
    void completedVerificationCannotOverrideStaleManifest() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        assertFalse(hasQualityBlocker());

        changeDestination("Houston, TX");

        assertManifestBlocker("MANIFEST_STALE");
    }

    @Test
    void replacementManifestRequiresNewQualityVerification() {

        MovementManifest original = generateIssuedManifest();

        completeQualityVerification(original);

        assertFalse(hasQualityBlocker());

        MovementManifest replacement = generateIssuedManifest();

        assertEquals(
                ManifestStatus.SUPERSEDED,
                lifecycleService.getLifecycle(
                        original.id()
                ).status()
        );

        assertTrue(hasQualityBlocker());

        completeQualityVerification(replacement);

        assertFalse(hasQualityBlocker());
    }

    @Test
    void newerDraftDoesNotInvalidateCompletedQualityVerification() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        generateManifest();

        assertNoManifestBlockers();
        assertFalse(hasQualityBlocker());
    }

    @Test
    void restoringOriginalValuesDoesNotRestoreQualityVerification() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        assertFalse(hasQualityBlocker());

        changeDestination("Houston, TX");

        assertManifestBlocker("MANIFEST_STALE");

        changeDestination("Dallas, TX");

        assertNoManifestBlockers();
        assertTrue(hasQualityBlocker());
    }

    @Test
    void reverificationAfterOperationalChangesClearsQualityBlocker() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        changeDestination("Houston, TX");
        changeDestination("Dallas, TX");

        assertTrue(hasQualityBlocker());

        completeQualityVerification(manifest);

        assertNoManifestBlockers();
        assertFalse(hasQualityBlocker());
    }

    @Test
    void unrelatedEquipmentDoesNotInvalidateQualityVerification() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        UUID unrelatedEquipmentId = registerEquipment();

        jdbcTemplate.update(
                """
                UPDATE equipment
                SET name = ?
                WHERE id = ?
                """,
                "Updated Unrelated Equipment",
                unrelatedEquipmentId
        );

        assertNoManifestBlockers();
        assertFalse(hasQualityBlocker());
    }

    @Test
    void assignedEquipmentChangeInvalidatesQualityVerification() {

        UUID equipmentId = registerEquipment();

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
        );

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        assertFalse(hasQualityBlocker());

        jdbcTemplate.update(
                """
                UPDATE equipment
                SET name = ?
                WHERE id = ?
                """,
                "Modified Trailer",
                equipmentId
        );

        assertManifestBlocker("MANIFEST_STALE");

        jdbcTemplate.update(
                """
                UPDATE equipment
                SET name = ?
                WHERE id = ?
                """,
                "Dispatch Assessment Test Trailer",
                equipmentId
        );

        assertNoManifestBlockers();
        assertTrue(hasQualityBlocker());
    }

    @Test
    void equipmentCapabilityChangeInvalidatesQualityVerification() {

        UUID equipmentId = registerEquipment();

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
        );

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        assertFalse(hasQualityBlocker());

        jdbcTemplate.update(
                """
                INSERT INTO equipment_capabilities (
                    equipment_id,
                    capability
                )
                VALUES (?, ?)
                """,
                equipmentId,
                "TEST_CAPABILITY"
        );

        assertTrue(hasQualityBlocker());
    }

    @Test
    void noOpEquipmentUpdatePreservesQualityVerification() {

        UUID equipmentId = registerEquipment();

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
        );

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        jdbcTemplate.update(
                """
                UPDATE equipment
                SET name = name
                WHERE id = ?
                """,
                equipmentId
        );

        assertNoManifestBlockers();
        assertFalse(hasQualityBlocker());
    }

    private long currentOperationalRevision() {

        return jdbcTemplate.queryForObject(
                """
                SELECT operational_revision
                FROM movements
                WHERE id = ?
                """,
                Long.class,
                movement.id()
        );
    }

    private UUID registerCargo() {

        UUID cargoId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO cargo (
                    id,
                    name,
                    weight_grams,
                    length_mm,
                    width_mm,
                    height_mm
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                cargoId,
                "Revision Test Cargo",
                25000L,
                500L,
                500L,
                500L
        );

        return cargoId;
    }

    private UUID findLoadingPlanId() {

        return jdbcTemplate.queryForObject(
                """
                SELECT id
                FROM loading_plans
                WHERE movement_id = ?
                """,
                UUID.class,
                movement.id()
        );
    }

    private UUID assignTestCargo(UUID cargoId) {

        UUID assignmentId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO loading_plan_cargo (
                    id,
                    loading_plan_id,
                    cargo_id,
                    quantity
                )
                VALUES (?, ?, ?, ?)
                """,
                assignmentId,
                findLoadingPlanId(),
                cargoId,
                2
        );

        return assignmentId;
    }

    @Test
    void loadingPlanCreationIncrementsOperationalRevision() {

        long originalRevision = currentOperationalRevision();

        addLoadingPlanWithoutArrangement();

        assertTrue(
                currentOperationalRevision() > originalRevision
        );
    }

    @Test
    void cargoQuantityChangeAndRestorationIncrementRevision() {

        addLoadingPlanWithoutArrangement();

        UUID assignmentId = assignTestCargo(registerCargo());

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                UPDATE loading_plan_cargo
                SET quantity = 3
                WHERE id = ?
                """,
                assignmentId
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );

        jdbcTemplate.update(
                """
                UPDATE loading_plan_cargo
                SET quantity = 2
                WHERE id = ?
                """,
                assignmentId
        );

        assertEquals(
                originalRevision + 2,
                currentOperationalRevision()
        );
    }

    @Test
    void assignedCargoCatalogChangeIncrementsRevision() {

        addLoadingPlanWithoutArrangement();

        UUID cargoId = registerCargo();

        assignTestCargo(cargoId);

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                UPDATE cargo
                SET weight_grams = 30000
                WHERE id = ?
                """,
                cargoId
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );
    }

    @Test
    void unrelatedCargoChangePreservesOperationalRevision() {

        UUID cargoId = registerCargo();

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                UPDATE cargo
                SET weight_grams = 30000
                WHERE id = ?
                """,
                cargoId
        );

        assertEquals(
                originalRevision,
                currentOperationalRevision()
        );
    }

    @Test
    void cargoRemovalIncrementsOperationalRevision() {

        addLoadingPlanWithoutArrangement();

        UUID assignmentId = assignTestCargo(registerCargo());

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                DELETE FROM loading_plan_cargo
                WHERE id = ?
                """,
                assignmentId
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );
    }

    private void saveTestArrangement() {

        jdbcTemplate.update(
                """
                INSERT INTO loading_plan_arrangements (
                    loading_plan_id,
                    format_version,
                    request_snapshot,
                    preview_snapshot,
                    status,
                    generated_at
                )
                VALUES (
                    ?, 1, '{}'::jsonb, '{}'::jsonb,
                    'VALID', CURRENT_TIMESTAMP
                )
                """,
                findLoadingPlanId()
        );
    }
    @Test
    void loadingArrangementCreationIncrementsRevision() {

        addLoadingPlanWithoutArrangement();

        long originalRevision = currentOperationalRevision();

        saveTestArrangement();

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );
    }

    @Test
    void loadingArrangementModificationIncrementsRevision() {

        addLoadingPlanWithoutArrangement();

        saveTestArrangement();

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                UPDATE loading_plan_arrangements
                SET status = 'INVALID'
                WHERE loading_plan_id = ?
                """,
                findLoadingPlanId()
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );

        jdbcTemplate.update(
                """
                UPDATE loading_plan_arrangements
                SET status = 'VALID'
                WHERE loading_plan_id = ?
                """,
                findLoadingPlanId()
        );

        assertEquals(
                originalRevision + 2,
                currentOperationalRevision()
        );
    }

    @Test
    void loadingArrangementRemovalIncrementsRevision() {

        addLoadingPlanWithoutArrangement();

        saveTestArrangement();

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                DELETE FROM loading_plan_arrangements
                WHERE loading_plan_id = ?
                """,
                findLoadingPlanId()
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );
    }

    private UUID registerAssignedEquipment() {

        UUID equipmentId = registerEquipment();

        assignmentService.assign(
                movement.id(),
                equipmentId,
                MovementEquipmentRole.CARGO_CARRIER
        );

        return equipmentId;
    }
    @Test
    void equipmentSpecificationChangesIncrementRevision() {

        UUID equipmentId = registerAssignedEquipment();

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                INSERT INTO equipment_specifications (
                    equipment_id,
                    length_mm,
                    width_mm,
                    height_mm,
                    weight_specifications_present
                )
                VALUES (?, 7000, 2400, 2500, false)
                """,
                equipmentId
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );

        jdbcTemplate.update(
                """
                UPDATE equipment_specifications
                SET length_mm = 7500
                WHERE equipment_id = ?
                """,
                equipmentId
        );

        assertEquals(
                originalRevision + 2,
                currentOperationalRevision()
        );
    }

    @Test
    void equipmentConfigurationChangesIncrementRevision() {

        UUID equipmentId = registerAssignedEquipment();
        UUID configurationId = UUID.randomUUID();

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                INSERT INTO equipment_configurations (
                    id, equipment_id, name, active
                )
                VALUES (?, ?, ?, true)
                """,
                configurationId,
                equipmentId,
                "Test Configuration"
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );

        jdbcTemplate.update(
                """
                UPDATE equipment_configurations
                SET active = false
                WHERE id = ?
                """,
                configurationId
        );

        assertEquals(
                originalRevision + 2,
                currentOperationalRevision()
        );
    }

    @Test
    void configurationCapabilityChangesIncrementRevision() {

        UUID equipmentId = registerAssignedEquipment();
        UUID configurationId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment_configurations (
                    id, equipment_id, name, active
                )
                VALUES (?, ?, ?, true)
                """,
                configurationId,
                equipmentId,
                "Test Configuration"
        );

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                INSERT INTO equipment_configuration_capabilities (
                    configuration_id, capability
                )
                VALUES (?, ?)
                """,
                configurationId,
                "TEST_CAPABILITY"
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );
    }

    @Test
    void equipmentModificationChangesIncrementRevision() {

        UUID equipmentId = registerAssignedEquipment();
        UUID modificationId = UUID.randomUUID();

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                INSERT INTO equipment_modifications (
                    id,
                    equipment_id,
                    type,
                    description,
                    effective_at,
                    recorded_at
                )
                VALUES (
                    ?, ?, ?, ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                modificationId,
                equipmentId,
                "TEST",
                "Test equipment modification"
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );

        jdbcTemplate.update(
                """
                UPDATE equipment_modifications
                SET removed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                modificationId
        );

        assertEquals(
                originalRevision + 2,
                currentOperationalRevision()
        );
    }

    @Test
    void equipmentModificationEventIncrementsRevision() {

        UUID equipmentId = registerAssignedEquipment();
        UUID modificationId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                INSERT INTO equipment_modifications (
                    id,
                    equipment_id,
                    type,
                    description,
                    effective_at,
                    recorded_at
                )
                VALUES (
                    ?, ?, ?, ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                modificationId,
                equipmentId,
                "TEST",
                "Test equipment modification"
        );

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                INSERT INTO equipment_modification_events (
                    id,
                    modification_id,
                    type,
                    effective_at,
                    recorded_at
                )
                VALUES (
                    ?, ?, ?,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                """,
                UUID.randomUUID(),
                modificationId,
                "TEST"
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );
    }

    @Test
    void cargoSpaceDimensionChangeIncrementsRevision() {

        addLoadingPlanWithoutArrangement();

        long originalRevision = currentOperationalRevision();

        jdbcTemplate.update(
                """
                UPDATE equipment_cargo_spaces
                SET usable_length_mm = usable_length_mm + 500
                WHERE id = (
                    SELECT cargo_space_id
                    FROM loading_plans
                    WHERE id = ?
                )
                """,
                findLoadingPlanId()
        );

        assertEquals(
                originalRevision + 1,
                currentOperationalRevision()
        );
    }

    @Test
    void rolledBackOperationalChangePreservesVerification() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        long originalRevision = currentOperationalRevision();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_NESTED
        );

        transaction.executeWithoutResult(status -> {

            changeDestination("Houston, TX");

            assertEquals(
                    originalRevision + 1,
                    currentOperationalRevision()
            );

            assertManifestBlocker("MANIFEST_STALE");

            status.setRollbackOnly();
        });

        assertEquals(
                originalRevision,
                currentOperationalRevision()
        );

        assertNoManifestBlockers();
        assertFalse(hasQualityBlocker());
    }

    @Test
    void rolledBackCatalogChangePreservesVerification() {

        UUID equipmentId = registerAssignedEquipment();

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        long originalRevision = currentOperationalRevision();

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_NESTED
        );

        transaction.executeWithoutResult(status -> {

            jdbcTemplate.update(
                    """
                    UPDATE equipment
                    SET name = ?
                    WHERE id = ?
                    """,
                    "Temporary Equipment Name",
                    equipmentId
            );

            assertEquals(
                    originalRevision + 1,
                    currentOperationalRevision()
            );

            status.setRollbackOnly();
        });

        assertEquals(
                originalRevision,
                currentOperationalRevision()
        );

        assertNoManifestBlockers();
        assertFalse(hasQualityBlocker());
    }

    @Test
    void draftManifestIsNotEligibleForVerification() {

        MovementManifest manifest = generateManifest();

        assertEquals(
                VerificationStatus.NOT_ELIGIBLE,
                verificationStatus(manifest)
        );
    }

    @Test
    void currentIssuedManifestRequiresVerificationStatus() {

        MovementManifest manifest = generateIssuedManifest();

        assertEquals(
                VerificationStatus.VERIFICATION_REQUIRED,
                verificationStatus(manifest)
        );
    }

    @Test
    void completedVerificationReturnsVerifiedStatus() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        assertEquals(
                VerificationStatus.VERIFIED,
                verificationStatus(manifest)
        );
    }

    @Test
    void staleManifestReturnsNotEligibleStatus() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        changeDestination("Houston, TX");

        assertEquals(
                VerificationStatus.NOT_ELIGIBLE,
                verificationStatus(manifest)
        );
    }

    @Test
    void restoredOperationalValuesRequireReverificationStatus() {

        MovementManifest manifest = generateIssuedManifest();

        completeQualityVerification(manifest);

        changeDestination("Houston, TX");
        changeDestination("Dallas, TX");

        assertEquals(
                VerificationStatus.REVERIFICATION_REQUIRED,
                verificationStatus(manifest)
        );

        completeQualityVerification(manifest);

        assertEquals(
                VerificationStatus.VERIFIED,
                verificationStatus(manifest)
        );
    }

    @Test
    void supersededManifestReturnsNotEligibleStatus() {

        MovementManifest original = generateIssuedManifest();

        completeQualityVerification(original);

        MovementManifest replacement = generateIssuedManifest();

        assertEquals(
                VerificationStatus.NOT_ELIGIBLE,
                verificationStatus(original)
        );

        assertEquals(
                VerificationStatus.VERIFICATION_REQUIRED,
                verificationStatus(replacement)
        );
    }

}