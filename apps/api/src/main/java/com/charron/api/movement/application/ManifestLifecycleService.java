package com.charron.api.movement.application;

import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestLifecycle;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.MovementManifest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ManifestLifecycleService {

    private final MovementManifestRepository manifestRepository;

    private final ManifestLifecycleEventRepository eventRepository;

    private final MovementRepository movementRepository;

    private final MovementManifestService manifestService;

    public ManifestLifecycleService(
            MovementManifestRepository manifestRepository,
            ManifestLifecycleEventRepository eventRepository,
            MovementRepository movementRepository,
            MovementManifestService manifestService
    ) {

        this.manifestRepository = Objects.requireNonNull(
                manifestRepository,
                "Manifest repository is required"
        );

        this.eventRepository = Objects.requireNonNull(
                eventRepository,
                "Lifecycle event repository is required"
        );

        this.movementRepository = Objects.requireNonNull(
                movementRepository,
                "Movement repository is required"
        );

        this.manifestService = Objects.requireNonNull(
                manifestService,
                "Manifest service is required"
        );
    }

    @Transactional(readOnly = true)
    public ManifestLifecycle getLifecycle(UUID manifestId) {

        MovementManifest manifest = requireManifest(manifestId);

        return reconstruct(manifest);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ManifestLifecycle review(
            UUID manifestId,
            UUID actorId,
            String remarks
    ) {

        Objects.requireNonNull(
                actorId,
                "Actor ID is required"
        );

        MovementManifest manifest = requireManifest(manifestId);

        lockMovement(manifest.movementId());

        ManifestLifecycle lifecycle = reconstruct(manifest);

        requireTransition(
                lifecycle,
                ManifestStatus.REVIEWED
        );

        requireCurrentManifest(manifestId);

        return recordTransition(
                manifest,
                lifecycle,
                ManifestStatus.REVIEWED,
                actorId,
                currentTimestamp(),
                remarks
        );
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ManifestLifecycle issue(
            UUID manifestId,
            UUID actorId,
            String remarks
    ) {

        Objects.requireNonNull(
                actorId,
                "Actor ID is required"
        );

        MovementManifest manifest = requireManifest(manifestId);

        lockMovement(manifest.movementId());

        ManifestLifecycle lifecycle = reconstruct(manifest);

        requireTransition(
                lifecycle,
                ManifestStatus.ISSUED
        );

        requireCurrentManifest(manifestId);

        List<MovementManifest> history =
                manifestRepository.findByMovementId(
                        manifest.movementId()
                );

        MovementManifest previouslyIssued = null;

        ManifestLifecycle previousLifecycle = null;

        for (MovementManifest candidate : history) {

            if (candidate.id().equals(manifest.id())) {
                continue;
            }

            ManifestLifecycle candidateLifecycle =
                    reconstruct(candidate);

            if (candidate.version() > manifest.version()
                    && (
                    candidateLifecycle.status() == ManifestStatus.ISSUED
                            || candidateLifecycle.status()
                            == ManifestStatus.SUPERSEDED
            )) {

                throw new ManifestLifecycleConflictException(
                        manifestId,
                        "Cannot issue an older manifest version "
                                + "after a newer version has been issued"
                );
            }

            if (!candidateLifecycle.isIssued()) {
                continue;
            }

            if (previouslyIssued != null) {

                throw new ManifestLifecycleConflictException(
                        manifestId,
                        "Multiple active issued manifests exist "
                                + "for this movement"
                );
            }

            previouslyIssued = candidate;
            previousLifecycle = candidateLifecycle;
        }

        Instant recordedAt = currentTimestamp();

        requireValidTimestamp(
                lifecycle,
                recordedAt
        );

        if (previouslyIssued != null) {

            requireValidTimestamp(
                    previousLifecycle,
                    recordedAt
            );

            recordTransition(
                    previouslyIssued,
                    previousLifecycle,
                    ManifestStatus.SUPERSEDED,
                    actorId,
                    recordedAt,
                    "Superseded by manifest version "
                            + manifest.version()
                            + " (" + manifest.id() + ")"
            );
        }

        return recordTransition(
                manifest,
                lifecycle,
                ManifestStatus.ISSUED,
                actorId,
                recordedAt,
                remarks
        );
    }

    private ManifestLifecycle recordTransition(
            MovementManifest manifest,
            ManifestLifecycle lifecycle,
            ManifestStatus targetStatus,
            UUID actorId,
            Instant recordedAt,
            String remarks
    ) {

        requireTransition(
                lifecycle,
                targetStatus
        );

        requireValidTimestamp(
                lifecycle,
                recordedAt
        );

        ManifestLifecycleEvent event =
                new ManifestLifecycleEvent(
                        UUID.randomUUID(),
                        manifest.id(),
                        lifecycle.status(),
                        targetStatus,
                        actorId,
                        recordedAt,
                        remarks
                );

        List<ManifestLifecycleEvent> updatedEvents =
                new ArrayList<>(lifecycle.events());

        updatedEvents.add(event);

        ManifestLifecycle updatedLifecycle =
                ManifestLifecycle.reconstruct(
                        manifest,
                        updatedEvents
                );

        eventRepository.save(event);

        return updatedLifecycle;
    }

    private void requireCurrentManifest(UUID manifestId) {

        ManifestFreshnessAssessment assessment =
                manifestService.assessFreshness(manifestId);

        if (!assessment.isCurrent()) {

            throw new ManifestFreshnessConflictException(
                    assessment
            );
        }
    }

    private void requireTransition(
            ManifestLifecycle lifecycle,
            ManifestStatus targetStatus
    ) {

        if (!lifecycle.canTransitionTo(targetStatus)) {

            throw new ManifestLifecycleConflictException(
                    lifecycle.manifestId(),
                    "Manifest cannot transition from "
                            + lifecycle.status()
                            + " to "
                            + targetStatus
            );
        }
    }

    private void requireValidTimestamp(
            ManifestLifecycle lifecycle,
            Instant recordedAt
    ) {

        if (recordedAt.isBefore(
                lifecycle.lastTransitionAt()
        )) {

            throw new ManifestLifecycleConflictException(
                    lifecycle.manifestId(),
                    "Transition timestamp cannot precede "
                            + "the previous lifecycle timestamp"
            );
        }
    }

    private void lockMovement(UUID movementId) {

        movementRepository
                .findByIdForUpdate(movementId)
                .orElseThrow(
                        () -> new MovementNotFoundException(
                                movementId
                        )
                );
    }

    private Instant currentTimestamp() {

        return Instant.now()
                .truncatedTo(ChronoUnit.MICROS);
    }

    private MovementManifest requireManifest(UUID manifestId) {

        Objects.requireNonNull(
                manifestId,
                "Manifest ID is required"
        );

        return manifestRepository
                .findById(manifestId)
                .orElseThrow(
                        () -> new ManifestNotFoundException(manifestId)
                );
    }

    private ManifestLifecycle reconstruct(
            MovementManifest manifest
    ) {

        return ManifestLifecycle.reconstruct(
                manifest,
                eventRepository.findByManifestId(manifest.id())
        );
    }
}