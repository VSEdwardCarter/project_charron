package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.MovementQualityVerification;
import com.charron.api.movement.domain.ManifestFreshnessStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovementQualityVerificationServiceTest {

    private MovementRepository movementRepository;

    private MovementManifestRepository manifestRepository;

    private ManifestLifecycleService lifecycleService;

    private MovementManifestService manifestService;

    private MovementQualityVerificationRepository verificationRepository;

    private MovementQualityVerificationService service;

    private Movement movement;

    private MovementManifest manifest;

    private ManifestLifecycle lifecycle;

    private ManifestFreshnessAssessment freshness;

    private final UUID movementId = UUID.randomUUID();

    private final UUID manifestId = UUID.randomUUID();

    private final UUID reviewerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        movementRepository =
                mock(MovementRepository.class);

        manifestRepository =
                mock(MovementManifestRepository.class);

        lifecycleService =
                mock(ManifestLifecycleService.class);

        manifestService =
                mock(MovementManifestService.class);

        verificationRepository =
                mock(MovementQualityVerificationRepository.class);

        movement = mock(Movement.class);

        manifest = mock(MovementManifest.class);

        lifecycle = mock(ManifestLifecycle.class);

        freshness = mock(ManifestFreshnessAssessment.class);

        service = new MovementQualityVerificationService(
                movementRepository,
                manifestRepository,
                lifecycleService,
                manifestService,
                verificationRepository
        );
    }

    @Test
    void completesVerificationForCurrentIssuedManifest() {

        configureEligibleManifest();

        var verification = completeVerification(
                completedChecks(),
                "  Physical inspection completed  "
        );

        assertAll(
                () -> assertNotNull(verification.id()),
                () -> assertEquals(
                        manifestId,
                        verification.manifestId()
                ),
                () -> assertEquals(
                        reviewerId,
                        verification.reviewerId()
                ),
                () -> assertNotNull(
                        verification.verifiedAt()
                ),
                () -> assertEquals(
                        "Physical inspection completed",
                        verification.remarks()
                ),
                () -> assertTrue(
                        verification.equipmentAssignmentVerified()
                ),
                () -> assertTrue(
                        verification.equipmentInspectionCompleted()
                ),
                () -> assertTrue(
                        verification.cargoReconciliationVerified()
                ),
                () -> assertTrue(
                        verification.loadingArrangementVerified()
                ),
                () -> assertTrue(
                        verification.cargoSecurementVerified()
                ),
                () -> assertTrue(
                        verification.documentationVerified()
                )
        );

        verify(verificationRepository).save(verification);
    }

    @Test
    void locksMovementBeforeEvaluatingManifest() {

        configureEligibleManifest();

        var verification = completeVerification(
                completedChecks(),
                null
        );

        var order = inOrder(
                movementRepository,
                manifestRepository,
                lifecycleService,
                manifestService,
                verificationRepository
        );

        order.verify(movementRepository)
                .findByIdForUpdate(movementId);

        order.verify(manifestRepository)
                .findById(manifestId);

        order.verify(lifecycleService)
                .getLifecycle(manifestId);

        order.verify(manifestService)
                .assessFreshness(manifestId);

        order.verify(verificationRepository)
                .save(verification);
    }

    @Test
    void rejectsMissingMovement() {

        when(movementRepository.findByIdForUpdate(movementId))
                .thenReturn(Optional.empty());

        assertThrows(
                MovementNotFoundException.class,
                () -> completeVerification(
                        completedChecks(),
                        null
                )
        );

        verifyNoInteractions(
                manifestRepository,
                lifecycleService,
                manifestService,
                verificationRepository
        );
    }

    @Test
    void rejectsMissingManifest() {

        configureMovement();

        when(manifestRepository.findById(manifestId))
                .thenReturn(Optional.empty());

        assertThrows(
                ManifestNotFoundException.class,
                () -> completeVerification(
                        completedChecks(),
                        null
                )
        );

        verifyNoInteractions(
                lifecycleService,
                manifestService,
                verificationRepository
        );
    }

    @Test
    void rejectsManifestBelongingToDifferentMovement() {

        configureMovement();

        when(manifestRepository.findById(manifestId))
                .thenReturn(Optional.of(manifest));

        when(manifest.movementId())
                .thenReturn(UUID.randomUUID());

        assertThrows(
                ManifestNotFoundException.class,
                () -> completeVerification(
                        completedChecks(),
                        null
                )
        );

        verifyNoInteractions(
                lifecycleService,
                manifestService,
                verificationRepository
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = ManifestStatus.class,
            names = {
                    "DRAFT",
                    "REVIEWED",
                    "SUPERSEDED"
            }
    )
    void rejectsManifestThatIsNotIssued(
            ManifestStatus status
    ) {

        configureMovement();
        configureManifest();

        when(lifecycleService.getLifecycle(manifestId))
                .thenReturn(lifecycle);

        when(lifecycle.status())
                .thenReturn(status);

        assertThrows(
                ManifestLifecycleConflictException.class,
                () -> completeVerification(
                        completedChecks(),
                        null
                )
        );

        verifyNoInteractions(
                manifestService,
                verificationRepository
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = ManifestFreshnessStatus.class,
            names = {
                    "STALE",
                    "UNVERIFIABLE"
            }
    )
    void rejectsManifestThatIsNotConfirmedCurrent(
            ManifestFreshnessStatus status
    ) {

        configureMovement();
        configureManifest();
        configureIssuedLifecycle();

        when(manifestService.assessFreshness(manifestId))
                .thenReturn(freshness);

        when(freshness.manifestId())
                .thenReturn(manifestId);

        when(freshness.isCurrent())
                .thenReturn(false);

        when(freshness.status())
                .thenReturn(status);

        when(freshness.changes())
                .thenReturn(java.util.Set.of());

        when(freshness.reason())
                .thenReturn("Manifest freshness cannot be confirmed");

        assertThrows(
                ManifestFreshnessConflictException.class,
                () -> completeVerification(
                        completedChecks(),
                        null
                )
        );

        verifyNoInteractions(verificationRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void rejectsIncompleteQualityVerification(
            int incompleteCheck
    ) {

        configureEligibleManifest();

        boolean[] checks = completedChecks();

        checks[incompleteCheck] = false;

        assertThrows(
                IllegalArgumentException.class,
                () -> completeVerification(checks, null)
        );

        verifyNoInteractions(verificationRepository);
    }

    @Test
    void rejectsMissingReviewerIdentity() {

        assertThrows(
                NullPointerException.class,
                () -> service.complete(
                        movementId,
                        manifestId,
                        null,
                        true, true, true,
                        true, true, true,
                        null
                )
        );

        verifyNoInteractions(
                movementRepository,
                manifestRepository,
                lifecycleService,
                manifestService,
                verificationRepository
        );
    }

    @Test
    void retrievesHistoricalVerifications() {

        configureManifest();

        MovementQualityVerification verification =
                mock(MovementQualityVerification.class);

        when(verificationRepository.findByManifestId(manifestId))
                .thenReturn(List.of(verification));

        var history = service.getHistory(
                movementId,
                manifestId
        );

        assertEquals(List.of(verification), history);

        verifyNoInteractions(
                lifecycleService,
                manifestService
        );
    }

    @Test
    void rejectsHistoricalRetrievalForDifferentMovement() {

        when(manifestRepository.findById(manifestId))
                .thenReturn(Optional.of(manifest));

        when(manifest.movementId())
                .thenReturn(UUID.randomUUID());

        assertThrows(
                ManifestNotFoundException.class,
                () -> service.getHistory(
                        movementId,
                        manifestId
                )
        );

        verifyNoInteractions(verificationRepository);
    }

    @Test
    void permitsMultipleIndependentVerificationRecords() {

        configureEligibleManifest();

        var first = completeVerification(
                completedChecks(),
                "Initial inspection"
        );

        var second = completeVerification(
                completedChecks(),
                "Repeat inspection"
        );

        assertNotEquals(first.id(), second.id());

        assertEquals(
                first.manifestId(),
                second.manifestId()
        );

        verify(verificationRepository).save(first);

        verify(verificationRepository).save(second);
    }

    private void configureMovement() {

        when(movementRepository.findByIdForUpdate(movementId))
                .thenReturn(Optional.of(movement));
    }

    private void configureManifest() {

        when(manifestRepository.findById(manifestId))
                .thenReturn(Optional.of(manifest));

        when(manifest.movementId())
                .thenReturn(movementId);
    }

    private void configureIssuedLifecycle() {

        when(lifecycleService.getLifecycle(manifestId))
                .thenReturn(lifecycle);

        when(lifecycle.status())
                .thenReturn(ManifestStatus.ISSUED);
    }

    private void configureEligibleManifest() {

        configureMovement();
        configureManifest();
        configureIssuedLifecycle();

        when(manifestService.assessFreshness(manifestId))
                .thenReturn(freshness);

        when(freshness.isCurrent())
                .thenReturn(true);
    }

    private boolean[] completedChecks() {

        return new boolean[]{
                true, true, true,
                true, true, true
        };
    }

    private MovementQualityVerification completeVerification(
            boolean[] checks,
            String remarks
    ) {

        return service.complete(
                movementId,
                manifestId,
                reviewerId,
                checks[0],
                checks[1],
                checks[2],
                checks[3],
                checks[4],
                checks[5],
                remarks
        );
    }
}