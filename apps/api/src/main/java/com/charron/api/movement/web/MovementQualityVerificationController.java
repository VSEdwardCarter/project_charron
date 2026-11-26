package com.charron.api.movement.web;

import com.charron.api.movement.application.ManifestFreshnessConflictException;
import com.charron.api.movement.application.ManifestLifecycleConflictException;
import com.charron.api.movement.application.ManifestNotFoundException;
import com.charron.api.movement.application.MovementQualityVerificationService;
import com.charron.api.movement.domain.MovementQualityVerification;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping(
        "/api/movements/{movementId}/manifests/{manifestId}/quality-verifications"
)
public class MovementQualityVerificationController {

    private final MovementQualityVerificationService verificationService;

    public record VerificationStatusResponse(
            MovementQualityVerificationService.VerificationStatus status
    ) {}

    public MovementQualityVerificationController(
            MovementQualityVerificationService verificationService
    ) {
        this.verificationService =
                Objects.requireNonNull(verificationService);
    }

    @GetMapping
    public List<MovementQualityVerification> list(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId
    ) {

        return verificationService.getHistory(
                movementId,
                manifestId
        );
    }

    @GetMapping("/status")
    public VerificationStatusResponse getStatus(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId
    ) {

        return new VerificationStatusResponse(
                verificationService.getVerificationStatus(
                        movementId,
                        manifestId
                )
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementQualityVerification complete(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId,
            @RequestBody VerificationRequest request
    ) {

        if (request.reviewerId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Reviewer ID is required"
            );
        }

        if (!request.equipmentAssignmentVerified()
                || !request.equipmentInspectionCompleted()
                || !request.cargoReconciliationVerified()
                || !request.loadingArrangementVerified()
                || !request.cargoSecurementVerified()
                || !request.documentationVerified()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All required quality checks must be completed"
            );
        }

        return verificationService.complete(
                movementId,
                manifestId,
                request.reviewerId(),
                request.equipmentAssignmentVerified(),
                request.equipmentInspectionCompleted(),
                request.cargoReconciliationVerified(),
                request.loadingArrangementVerified(),
                request.cargoSecurementVerified(),
                request.documentationVerified(),
                request.remarks()
        );
    }

    @ExceptionHandler(ManifestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(
            ManifestNotFoundException exception
    ) {
        return new ErrorResponse(
                "MANIFEST_NOT_FOUND",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ManifestLifecycleConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleLifecycleConflict(
            ManifestLifecycleConflictException exception
    ) {
        return new ErrorResponse(
                "MANIFEST_LIFECYCLE_CONFLICT",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ManifestFreshnessConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFreshnessConflict(
            ManifestFreshnessConflictException exception
    ) {
        return new ErrorResponse(
                "MANIFEST_FRESHNESS_CONFLICT",
                exception.getMessage()
        );
    }

    public record VerificationRequest(
            UUID reviewerId,
            boolean equipmentAssignmentVerified,
            boolean equipmentInspectionCompleted,
            boolean cargoReconciliationVerified,
            boolean loadingArrangementVerified,
            boolean cargoSecurementVerified,
            boolean documentationVerified,
            String remarks
    ) {}

    public record ErrorResponse(
            String code,
            String message
    ) {}
}