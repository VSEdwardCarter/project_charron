
package com.charron.api.movement.application;

import com.charron.api.loading.application.*;
import com.charron.api.loading.domain.*;
import com.charron.api.movement.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DispatchAssessmentServiceTest {

    private MovementRepository movementRepository;
    private MovementEquipmentAssignmentRepository equipmentRepository;
    private LoadingPlanService loadingPlanService;
    private LoadingPlanCargoRepository cargoRepository;
    private LoadingPlanArrangementRepository arrangementRepository;
    private LoadingPlanArrangementFreshnessService freshnessService;
    private MovementManifestRepository manifestRepository;
    private ManifestLifecycleService lifecycleService;
    private MovementManifestService manifestService;
    private MovementQualityVerificationRepository verificationRepository;
    private MovementManifest issuedManifest;
    private ManifestLifecycle issuedLifecycle;

    private DispatchAssessmentService service;

    private UUID movementId;
    private UUID equipmentId;
    private LoadingPlan plan;

    @BeforeEach
    void setUp() {
        movementRepository = mock(MovementRepository.class);
        equipmentRepository =
                mock(MovementEquipmentAssignmentRepository.class);
        loadingPlanService = mock(LoadingPlanService.class);
        cargoRepository = mock(LoadingPlanCargoRepository.class);
        arrangementRepository =
                mock(LoadingPlanArrangementRepository.class);
        freshnessService =
                mock(LoadingPlanArrangementFreshnessService.class);

        manifestRepository = mock(MovementManifestRepository.class);
        lifecycleService = mock(ManifestLifecycleService.class);
        manifestService = mock(MovementManifestService.class);
        verificationRepository =
                mock(MovementQualityVerificationRepository.class);

        service = new DispatchAssessmentService(
                movementRepository,
                equipmentRepository,
                loadingPlanService,
                cargoRepository,
                arrangementRepository,
                freshnessService,
                manifestRepository,
                lifecycleService,
                manifestService,
                verificationRepository
        );

        movementId = UUID.randomUUID();
        equipmentId = UUID.randomUUID();

        configureIssuedManifest();

        plan = LoadingPlan.create(
                movementId,
                equipmentId,
                UUID.randomUUID()
        );

        Movement movement = movement(MovementStatus.PLANNED);

        when(movementRepository.findById(movementId))
                .thenReturn(Optional.of(movement));

        when(equipmentRepository.findByMovementId(movementId))
                .thenReturn(List.of(
                        MovementEquipmentAssignment.assign(
                                movementId,
                                equipmentId,
                                MovementEquipmentRole.CARGO_CARRIER
                        )
                ));

        when(loadingPlanService.getByMovementId(movementId))
                .thenReturn(List.of(plan));

        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of(
                        LoadingPlanCargo.create(
                                plan.id(),
                                UUID.randomUUID(),
                                2
                        )
                ));

        LoadingPlanArrangement arrangement =
                arrangement(plan.id(), "VALID");

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(arrangement));

        when(freshnessService.isCurrent(arrangement))
                .thenReturn(true);
    }

    private void configureIssuedManifest() {

        issuedManifest = mock(MovementManifest.class);
        issuedLifecycle = mock(ManifestLifecycle.class);

        UUID manifestId = UUID.randomUUID();

        when(issuedManifest.id()).thenReturn(manifestId);
        when(issuedManifest.version()).thenReturn(1);

        when(verificationRepository.existsForCurrentOperationalRevision(manifestId))
                .thenReturn(true);

        when(manifestRepository.findByMovementId(movementId))
                .thenReturn(List.of(issuedManifest));

        when(lifecycleService.getLifecycle(manifestId))
                .thenReturn(issuedLifecycle);

        when(issuedLifecycle.status())
                .thenReturn(ManifestStatus.ISSUED);

        when(manifestService.assessFreshness(manifestId))
                .thenReturn(new ManifestFreshnessAssessment(
                        manifestId,
                        ManifestFreshnessStatus.CURRENT,
                        java.util.Set.of(),
                        Instant.now(),
                        null
                ));
    }

    private Movement movement(MovementStatus status) {
        Instant created = Instant.parse("2026-11-20T10:00:00Z");

        return new Movement(
                movementId,
                "MOV-ASSESS-001",
                "Denver, CO",
                "Dallas, TX",
                Instant.parse("2026-12-01T08:00:00Z"),
                Instant.parse("2026-12-02T08:00:00Z"),
                status,
                created,
                created
        );
    }

    private LoadingPlanArrangement arrangement(
            UUID planId,
            String status
    ) {
        return LoadingPlanArrangement.create(
                planId,
                "{\"cargoSpaceId\":\""
                        + plan.cargoSpaceId() + "\",\"cargo\":[]}",
                "{\"status\":\"" + status + "\"}",
                status
        );
    }

    private boolean hasFinding(
            DispatchAssessment assessment,
            String code
    ) {
        return assessment.findings().stream()
                .anyMatch(finding -> finding.code().equals(code));
    }

    @Test
    void validPlanHasNoAutomatedBlockers() {
        DispatchAssessment result = service.assess(movementId);

        assertFalse(result.hasBlockingFindings());
        assertEquals(movementId, result.movementId());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void missingMovementThrowsNotFound() {
        when(movementRepository.findById(movementId))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> service.assess(movementId)
        );
    }

    @Test
    void movementNotPlannedIsBlocked() {
        when(movementRepository.findById(movementId))
                .thenReturn(Optional.of(
                        movement(MovementStatus.DISPATCHED)
                ));

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(result, "MOVEMENT_NOT_PLANNED"));
    }

    @Test
    void missingEquipmentIsBlocked() {
        when(equipmentRepository.findByMovementId(movementId))
                .thenReturn(List.of());

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(result, "EQUIPMENT_NOT_ASSIGNED"));
    }

    @Test
    void missingLoadingPlanIsBlocked() {
        when(loadingPlanService.getByMovementId(movementId))
                .thenReturn(List.of());

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(result, "LOADING_PLAN_MISSING"));
    }

    @Test
    void unassignedLoadingEquipmentIsBlocked() {
        when(equipmentRepository.findByMovementId(movementId))
                .thenReturn(List.of(
                        MovementEquipmentAssignment.assign(
                                movementId,
                                UUID.randomUUID(),
                                MovementEquipmentRole.POWER_UNIT
                        )
                ));

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(
                result,
                "LOADING_EQUIPMENT_NOT_ASSIGNED"
        ));
    }

    @Test
    void missingCargoIsBlocked() {
        when(cargoRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(List.of());

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(result, "LOADING_CARGO_MISSING"));
    }

    @Test
    void missingArrangementIsBlocked() {
        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.empty());

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(
                result,
                "LOADING_ARRANGEMENT_MISSING"
        ));
    }

    @Test
    void invalidArrangementIsBlocked() {
        LoadingPlanArrangement invalid =
                arrangement(plan.id(), "INVALID");

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(invalid));

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(
                result,
                "LOADING_ARRANGEMENT_INVALID"
        ));

        verify(freshnessService, never()).isCurrent(invalid);
    }

    @Test
    void staleArrangementIsBlocked() {
        LoadingPlanArrangement saved =
                arrangement(plan.id(), "VALID");

        when(arrangementRepository.findByLoadingPlanId(plan.id()))
                .thenReturn(Optional.of(saved));

        when(freshnessService.isCurrent(saved))
                .thenReturn(false);

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(
                result,
                "LOADING_ARRANGEMENT_STALE"
        ));
    }

    @Test
    void evaluatesEveryLoadingPlan() {
        LoadingPlan secondPlan = LoadingPlan.create(
                movementId,
                equipmentId,
                UUID.randomUUID()
        );

        when(loadingPlanService.getByMovementId(movementId))
                .thenReturn(List.of(plan, secondPlan));

        when(cargoRepository.findByLoadingPlanId(secondPlan.id()))
                .thenReturn(List.of());

        when(arrangementRepository.findByLoadingPlanId(secondPlan.id()))
                .thenReturn(Optional.empty());

        DispatchAssessment result = service.assess(movementId);

        assertTrue(hasFinding(result, "LOADING_CARGO_MISSING"));
        assertTrue(hasFinding(result, "LOADING_ARRANGEMENT_MISSING"));
    }

    @Test
    void missingEquipmentAndLoadingPlanReturnMultipleFindings() {
        when(equipmentRepository.findByMovementId(movementId))
                .thenReturn(List.of());

        when(loadingPlanService.getByMovementId(movementId))
                .thenReturn(List.of());

        DispatchAssessment result = service.assess(movementId);

        assertEquals(2, result.findings().size());
        assertTrue(result.hasBlockingFindings());
    }

    private void assertManifestBlocker(String code) {

        DispatchAssessment result = service.assess(movementId);

        assertTrue(result.hasBlockingFindings());

        assertEquals(1, result.findings().size());

        assertEquals(
                code,
                result.findings().getFirst().code()
        );

        assertEquals(
                DispatchAssessment.Severity.BLOCKER,
                result.findings().getFirst().severity()
        );
    }

    private MovementManifest configureAdditionalManifest(
            int version,
            ManifestStatus status
    ) {

        MovementManifest manifest =
                mock(MovementManifest.class);

        ManifestLifecycle lifecycle =
                mock(ManifestLifecycle.class);

        UUID manifestId = UUID.randomUUID();

        when(manifest.id()).thenReturn(manifestId);
        when(manifest.version()).thenReturn(version);

        when(lifecycle.status()).thenReturn(status);

        when(lifecycleService.getLifecycle(manifestId))
                .thenReturn(lifecycle);

        return manifest;
    }

    @Test
    void missingManifestIsBlocked() {

        when(manifestRepository.findByMovementId(movementId))
                .thenReturn(List.of());

        assertManifestBlocker("MANIFEST_NOT_ISSUED");

        verifyNoInteractions(manifestService);
    }

    @Test
    void draftManifestIsBlocked() {

        when(issuedLifecycle.status())
                .thenReturn(ManifestStatus.DRAFT);

        assertManifestBlocker("MANIFEST_NOT_ISSUED");

        verifyNoInteractions(manifestService);
    }

    @Test
    void reviewedManifestIsBlocked() {

        when(issuedLifecycle.status())
                .thenReturn(ManifestStatus.REVIEWED);

        assertManifestBlocker("MANIFEST_NOT_ISSUED");

        verifyNoInteractions(manifestService);
    }

    @Test
    void supersededManifestIsBlocked() {

        when(issuedLifecycle.status())
                .thenReturn(ManifestStatus.SUPERSEDED);

        assertManifestBlocker("MANIFEST_NOT_ISSUED");

        verifyNoInteractions(manifestService);
    }

    @Test
    void currentVerifiedIssuedManifestDoesNotCreateBlocker() {

        DispatchAssessment result =
                service.assess(movementId);

        assertFalse(result.hasBlockingFindings());
        assertTrue(result.findings().isEmpty());

        verify(manifestService)
                .assessFreshness(issuedManifest.id());
    }

    @Test
    void staleIssuedManifestIsBlocked() {

        UUID manifestId = issuedManifest.id();

        when(manifestService.assessFreshness(
                issuedManifest.id()
        )).thenReturn(new ManifestFreshnessAssessment(
                manifestId,
                ManifestFreshnessStatus.STALE,
                java.util.Set.of(ManifestChangeType.CARGO),
                Instant.now(),
                null
        ));

        assertManifestBlocker("MANIFEST_STALE");
    }

    @Test
    void unverifiableIssuedManifestIsBlocked() {

        UUID manifestId = issuedManifest.id();

        when(manifestService.assessFreshness(
                issuedManifest.id()
        )).thenReturn(new ManifestFreshnessAssessment(
                manifestId,
                ManifestFreshnessStatus.UNVERIFIABLE,
                java.util.Set.of(),
                Instant.now(),
                "Required loading arrangement is missing"
        ));

        assertManifestBlocker("MANIFEST_UNVERIFIABLE");
    }

    @Test
    void multipleIssuedManifestsAreBlocked() {

        MovementManifest second =
                configureAdditionalManifest(
                        2,
                        ManifestStatus.ISSUED
                );

        when(manifestRepository.findByMovementId(movementId))
                .thenReturn(List.of(second, issuedManifest));

        assertManifestBlocker("MANIFEST_ISSUANCE_CONFLICT");

        verifyNoInteractions(manifestService);
    }

    @Test
    void newerDraftDoesNotInvalidateCurrentIssuedManifest() {

        MovementManifest draft =
                configureAdditionalManifest(
                        2,
                        ManifestStatus.DRAFT
                );

        when(manifestRepository.findByMovementId(movementId))
                .thenReturn(List.of(draft, issuedManifest));

        DispatchAssessment result =
                service.assess(movementId);

        assertTrue(result.findings().isEmpty());

        verify(manifestService)
                .assessFreshness(issuedManifest.id());

        verify(manifestService, never())
                .assessFreshness(draft.id());
    }

    @Test
    void newerReviewedManifestDoesNotReplaceIssuedManifest() {

        MovementManifest reviewed =
                configureAdditionalManifest(
                        2,
                        ManifestStatus.REVIEWED
                );

        when(manifestRepository.findByMovementId(movementId))
                .thenReturn(List.of(reviewed, issuedManifest));

        DispatchAssessment result =
                service.assess(movementId);

        assertTrue(result.findings().isEmpty());

        verify(manifestService)
                .assessFreshness(issuedManifest.id());

        verify(manifestService, never())
                .assessFreshness(reviewed.id());
    }

    @Test
    void currentIssuedManifestWithoutVerificationIsBlocked() {

        UUID manifestId = issuedManifest.id();

        when(verificationRepository.existsForCurrentOperationalRevision(manifestId))
                .thenReturn(false);

        assertManifestBlocker("QUALITY_VERIFICATION_REQUIRED");
    }

    @Test
    void completedQualityVerificationClearsBlocker() {

        UUID manifestId = issuedManifest.id();

        when(verificationRepository.existsForCurrentOperationalRevision(manifestId))
                .thenReturn(true);

        DispatchAssessment result = service.assess(movementId);

        assertFalse(result.hasBlockingFindings());
        assertTrue(result.findings().isEmpty());

        verify(verificationRepository)
                .existsForCurrentOperationalRevision(manifestId);
    }

    @Test
    void completedVerificationCannotOverrideStaleManifest() {

        UUID manifestId = issuedManifest.id();

        when(manifestService.assessFreshness(manifestId))
                .thenReturn(new ManifestFreshnessAssessment(
                        manifestId,
                        ManifestFreshnessStatus.STALE,
                        java.util.Set.of(ManifestChangeType.CARGO),
                        Instant.now(),
                        null
                ));

        assertManifestBlocker("MANIFEST_STALE");

        verifyNoInteractions(verificationRepository);
    }

    @Test
    void previousVerificationDoesNotSatisfyReplacementManifest() {

        MovementManifest replacement =
                configureAdditionalManifest(
                        2,
                        ManifestStatus.ISSUED
                );

        UUID replacementId = replacement.id();

        when(issuedLifecycle.status())
                .thenReturn(ManifestStatus.SUPERSEDED);

        when(manifestRepository.findByMovementId(movementId))
                .thenReturn(List.of(replacement, issuedManifest));

        when(manifestService.assessFreshness(replacementId))
                .thenReturn(new ManifestFreshnessAssessment(
                        replacementId,
                        ManifestFreshnessStatus.CURRENT,
                        java.util.Set.of(),
                        Instant.now(),
                        null
                ));

        when(verificationRepository.existsForCurrentOperationalRevision(replacementId))
                .thenReturn(false);

        assertManifestBlocker("QUALITY_VERIFICATION_REQUIRED");

        verify(verificationRepository)
                .existsForCurrentOperationalRevision(replacementId);

        verify(verificationRepository, never())
                .existsForCurrentOperationalRevision(issuedManifest.id());
    }

}
