package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.MovementQualityVerification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MovementQualityVerificationService {

    public enum VerificationStatus {

        NOT_ELIGIBLE,

        VERIFICATION_REQUIRED,

        REVERIFICATION_REQUIRED,

        VERIFIED
    }

    private final MovementRepository movementRepository;

    private final MovementManifestRepository manifestRepository;

    private final ManifestLifecycleService lifecycleService;

    private final MovementManifestService manifestService;

    private final MovementQualityVerificationRepository verificationRepository;

    public MovementQualityVerificationService(
            MovementRepository movementRepository,
            MovementManifestRepository manifestRepository,
            ManifestLifecycleService lifecycleService,
            MovementManifestService manifestService,
            MovementQualityVerificationRepository verificationRepository
    ) {
        this.movementRepository =
                Objects.requireNonNull(movementRepository);

        this.manifestRepository =
                Objects.requireNonNull(manifestRepository);

        this.lifecycleService =
                Objects.requireNonNull(lifecycleService);

        this.manifestService =
                Objects.requireNonNull(manifestService);

        this.verificationRepository =
                Objects.requireNonNull(verificationRepository);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovementQualityVerification complete(
            UUID movementId,
            UUID manifestId,
            UUID reviewerId,
            boolean equipmentAssignmentVerified,
            boolean equipmentInspectionCompleted,
            boolean cargoReconciliationVerified,
            boolean loadingArrangementVerified,
            boolean cargoSecurementVerified,
            boolean documentationVerified,
            String remarks
    ) {

        Objects.requireNonNull(reviewerId, "Reviewer ID is required");

        // Serialize verification with operational changes
        // and manifest lifecycle transitions.
        movementRepository.findByIdForUpdate(movementId)
                .orElseThrow(
                        () -> new MovementNotFoundException(movementId)
                );

        requireManifest(movementId, manifestId);

        var lifecycle = lifecycleService.getLifecycle(manifestId);

        if (lifecycle.status() != ManifestStatus.ISSUED) {

            throw new ManifestLifecycleConflictException(
                    manifestId,
                    "Quality verification requires an issued manifest"
            );
        }

        var freshness = manifestService.assessFreshness(manifestId);

        if (!freshness.isCurrent()) {

            throw new ManifestFreshnessConflictException(
                    freshness
            );
        }

        MovementQualityVerification verification =
                new MovementQualityVerification(
                        UUID.randomUUID(),
                        manifestId,
                        reviewerId,
                        equipmentAssignmentVerified,
                        equipmentInspectionCompleted,
                        cargoReconciliationVerified,
                        loadingArrangementVerified,
                        cargoSecurementVerified,
                        documentationVerified,
                        Instant.now().truncatedTo(ChronoUnit.MICROS),
                        remarks
                );

        verificationRepository.save(verification);

        return verification;
    }

    @Transactional(readOnly = true)
    public List<MovementQualityVerification> getHistory(
            UUID movementId,
            UUID manifestId
    ) {

        requireManifest(movementId, manifestId);

        return verificationRepository.findByManifestId(manifestId);
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public VerificationStatus getVerificationStatus(
            UUID movementId,
            UUID manifestId
    ) {

        requireManifest(movementId, manifestId);

        var lifecycle =
                lifecycleService.getLifecycle(manifestId);

        if (lifecycle.status() != ManifestStatus.ISSUED) {
            return VerificationStatus.NOT_ELIGIBLE;
        }

        var freshness =
                manifestService.assessFreshness(manifestId);

        if (!freshness.isCurrent()) {
            return VerificationStatus.NOT_ELIGIBLE;
        }

        if (verificationRepository
                .existsForCurrentOperationalRevision(manifestId)) {

            return VerificationStatus.VERIFIED;
        }

        if (verificationRepository.existsByManifestId(manifestId)) {

            return VerificationStatus.REVERIFICATION_REQUIRED;
        }

        return VerificationStatus.VERIFICATION_REQUIRED;
    }

    private MovementManifest requireManifest(
            UUID movementId,
            UUID manifestId
    ) {

        return manifestRepository.findById(manifestId)
                .filter(manifest ->
                        manifest.movementId().equals(movementId)
                )
                .orElseThrow(
                        () -> new ManifestNotFoundException(manifestId)
                );
    }
}