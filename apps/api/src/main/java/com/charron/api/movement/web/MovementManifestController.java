package com.charron.api.movement.web;

import com.charron.api.movement.application.*;
import com.charron.api.movement.domain.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/movements/{movementId}/manifests")
public class MovementManifestController {

    private final MovementManifestService manifestService;
    private final MovementManifestRepository manifestRepository;
    private final MovementRepository movementRepository;
    private final ManifestLifecycleService lifecycleService;

    public MovementManifestController(
            MovementManifestService manifestService,
            MovementManifestRepository manifestRepository,
            MovementRepository movementRepository,
            ManifestLifecycleService lifecycleService
    ) {
        this.manifestService =
                Objects.requireNonNull(manifestService);

        this.manifestRepository =
                Objects.requireNonNull(manifestRepository);

        this.movementRepository =
                Objects.requireNonNull(movementRepository);

        this.lifecycleService =
                Objects.requireNonNull(lifecycleService);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovementManifest generate(
            @PathVariable UUID movementId
    ) {
        return manifestService.generateAndPersist(movementId);
    }

    @GetMapping
    public List<MovementManifest> list(
            @PathVariable UUID movementId
    ) {
        requireMovement(movementId);

        return manifestRepository.findByMovementId(movementId);
    }

    @GetMapping("/{manifestId}")
    public MovementManifest get(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId
    ) {
        return requireManifest(movementId, manifestId);
    }

    @GetMapping("/{manifestId}/lifecycle")
    public LifecycleResponse getLifecycle(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId
    ) {
        requireManifest(movementId, manifestId);

        return LifecycleResponse.from(
                lifecycleService.getLifecycle(manifestId)
        );
    }

    @GetMapping("/{manifestId}/freshness")
    public ManifestFreshnessAssessment getFreshness(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId
    ) {
        requireManifest(movementId, manifestId);

        return manifestService.assessFreshness(manifestId);
    }

    @PostMapping("/{manifestId}/review")
    public LifecycleResponse review(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId,
            @RequestBody TransitionRequest request
    ) {
        requireManifest(movementId, manifestId);
        requireActor(request);

        return LifecycleResponse.from(
                lifecycleService.review(
                        manifestId,
                        request.actorId(),
                        request.remarks()
                )
        );
    }

    @PostMapping("/{manifestId}/issue")
    public LifecycleResponse issue(
            @PathVariable UUID movementId,
            @PathVariable UUID manifestId,
            @RequestBody TransitionRequest request
    ) {
        requireManifest(movementId, manifestId);
        requireActor(request);

        return LifecycleResponse.from(
                lifecycleService.issue(
                        manifestId,
                        request.actorId(),
                        request.remarks()
                )
        );
    }

    @ExceptionHandler(ManifestFreshnessConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ConflictResponse handleFreshnessConflict(
            ManifestFreshnessConflictException exception
    ) {
        return new ConflictResponse(
                "MANIFEST_FRESHNESS_CONFLICT",
                exception.getMessage(),
                exception.getAssessment()
        );
    }

    @ExceptionHandler(ManifestLifecycleConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ConflictResponse handleLifecycleConflict(
            ManifestLifecycleConflictException exception
    ) {
        return new ConflictResponse(
                "MANIFEST_LIFECYCLE_CONFLICT",
                exception.getMessage(),
                null
        );
    }

    private MovementManifest requireManifest(
            UUID movementId,
            UUID manifestId
    ) {
        requireMovement(movementId);

        return manifestRepository.findById(manifestId)
                .filter(manifest ->
                        manifest.movementId().equals(movementId)
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Manifest not found for this movement"
                        )
                );
    }

    private void requireMovement(UUID movementId) {
        if (movementRepository.findById(movementId).isEmpty()) {
            throw new MovementNotFoundException(movementId);
        }
    }

    private void requireActor(TransitionRequest request) {
        if (request == null || request.actorId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Actor ID is required"
            );
        }
    }

    public record TransitionRequest(
            UUID actorId,
            String remarks
    ) {}

    public record LifecycleResponse(
            UUID manifestId,
            ManifestStatus status,
            Instant generatedAt,
            Instant lastTransitionAt,
            List<ManifestLifecycleEvent> events
    ) {
        public static LifecycleResponse from(
                ManifestLifecycle lifecycle
        ) {
            return new LifecycleResponse(
                    lifecycle.manifestId(),
                    lifecycle.status(),
                    lifecycle.generatedAt(),
                    lifecycle.lastTransitionAt(),
                    lifecycle.events()
            );
        }
    }

    public record ConflictResponse(
            String code,
            String message,
            ManifestFreshnessAssessment assessment
    ) {}
}