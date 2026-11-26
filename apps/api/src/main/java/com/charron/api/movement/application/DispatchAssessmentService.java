package com.charron.api.movement.application;

import com.charron.api.loading.application.LoadingPlanArrangementFreshnessService;
import com.charron.api.loading.application.LoadingPlanArrangementRepository;
import com.charron.api.loading.application.LoadingPlanCargoRepository;
import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.movement.domain.ManifestFreshnessAssessment;
import com.charron.api.movement.domain.ManifestStatus;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementManifest;
import com.charron.api.movement.domain.MovementStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class DispatchAssessmentService {

    private final MovementRepository movementRepository;

    private final MovementEquipmentAssignmentRepository
            equipmentRepository;

    private final LoadingPlanService loadingPlanService;

    private final LoadingPlanCargoRepository cargoRepository;

    private final LoadingPlanArrangementRepository
            arrangementRepository;

    private final LoadingPlanArrangementFreshnessService
            freshnessService;

    private final MovementManifestRepository manifestRepository;

    private final MovementQualityVerificationRepository
            verificationRepository;

    private final ManifestLifecycleService lifecycleService;

    private final MovementManifestService manifestService;

    public DispatchAssessmentService(
            MovementRepository movementRepository,
            MovementEquipmentAssignmentRepository equipmentRepository,
            LoadingPlanService loadingPlanService,
            LoadingPlanCargoRepository cargoRepository,
            LoadingPlanArrangementRepository arrangementRepository,
            LoadingPlanArrangementFreshnessService freshnessService,
            MovementManifestRepository manifestRepository,
            ManifestLifecycleService lifecycleService,
            MovementManifestService manifestService,
            MovementQualityVerificationRepository verificationRepository
    ) {
        this.movementRepository =
                Objects.requireNonNull(movementRepository);

        this.equipmentRepository =
                Objects.requireNonNull(equipmentRepository);

        this.loadingPlanService =
                Objects.requireNonNull(loadingPlanService);

        this.cargoRepository =
                Objects.requireNonNull(cargoRepository);

        this.arrangementRepository =
                Objects.requireNonNull(arrangementRepository);

        this.freshnessService =
                Objects.requireNonNull(freshnessService);

        this.manifestRepository =
                Objects.requireNonNull(manifestRepository);

        this.lifecycleService =
                Objects.requireNonNull(lifecycleService);

        this.manifestService =
                Objects.requireNonNull(manifestService);

        this.verificationRepository =
                Objects.requireNonNull(verificationRepository);
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public DispatchAssessment assess(UUID movementId) {

        Objects.requireNonNull(
                movementId,
                "Movement ID is required"
        );

        Movement movement = movementRepository
                .findById(movementId)
                .orElseThrow(
                        () -> new MovementNotFoundException(movementId)
                );

        List<DispatchAssessment.Finding> findings =
                new ArrayList<>();

        if (movement.status() != MovementStatus.PLANNED) {

            addBlocker(
                    findings,
                    "MOVEMENT_NOT_PLANNED",
                    "Movement is not in PLANNED status"
            );
        }

        List<MovementEquipmentAssignment> assignments =
                equipmentRepository.findByMovementId(movementId);

        if (assignments.isEmpty()) {

            addBlocker(
                    findings,
                    "EQUIPMENT_NOT_ASSIGNED",
                    "No equipment is assigned to this movement"
            );
        }

        Set<UUID> assignedEquipment = new HashSet<>();

        for (MovementEquipmentAssignment assignment : assignments) {

            assignedEquipment.add(
                    assignment.equipmentId()
            );
        }

        List<LoadingPlan> plans =
                loadingPlanService.getByMovementId(movementId);

        if (plans.isEmpty()) {

            addBlocker(
                    findings,
                    "LOADING_PLAN_MISSING",
                    "No loading plan exists for this movement"
            );
        }

        for (LoadingPlan plan : plans) {

            assessLoadingPlan(
                    plan,
                    assignedEquipment,
                    findings
            );
        }

        assessManifest(
                movementId,
                findings
        );

        return new DispatchAssessment(
                movementId,
                findings
        );
    }

    private void assessLoadingPlan(
            LoadingPlan plan,
            Set<UUID> assignedEquipment,
            List<DispatchAssessment.Finding> findings
    ) {

        UUID planId = plan.id();

        if (!assignedEquipment.contains(plan.equipmentId())) {

            addBlocker(
                    findings,
                    "LOADING_EQUIPMENT_NOT_ASSIGNED",
                    "Loading plan " + planId
                            + " references equipment that is not "
                            + "assigned to the movement"
            );
        }

        var cargoLines =
                cargoRepository.findByLoadingPlanId(planId);

        if (cargoLines.isEmpty()) {

            addBlocker(
                    findings,
                    "LOADING_CARGO_MISSING",
                    "Loading plan " + planId
                            + " has no assigned cargo"
            );
        }

        var savedArrangement =
                arrangementRepository.findByLoadingPlanId(planId);

        if (savedArrangement.isEmpty()) {

            addBlocker(
                    findings,
                    "LOADING_ARRANGEMENT_MISSING",
                    "Loading plan " + planId
                            + " has no saved loading arrangement"
            );

            return;
        }

        LoadingPlanArrangement arrangement =
                savedArrangement.orElseThrow();

        if (!"VALID".equals(arrangement.status())) {

            addBlocker(
                    findings,
                    "LOADING_ARRANGEMENT_INVALID",
                    "Loading plan " + planId
                            + " has an arrangement with status "
                            + arrangement.status()
            );

            return;
        }

        if (!freshnessService.isCurrent(arrangement)) {

            addBlocker(
                    findings,
                    "LOADING_ARRANGEMENT_STALE",
                    "Loading plan " + planId
                            + " has an outdated loading arrangement"
            );
        }
    }

    private void assessManifest(
            UUID movementId,
            List<DispatchAssessment.Finding> findings
    ) {

        List<MovementManifest> issuedManifests =
                new ArrayList<>();

        for (MovementManifest manifest :
                manifestRepository.findByMovementId(movementId)) {

            ManifestStatus status = lifecycleService
                    .getLifecycle(manifest.id())
                    .status();

            if (status == ManifestStatus.ISSUED) {

                issuedManifests.add(manifest);
            }
        }

        if (issuedManifests.isEmpty()) {

            addBlocker(
                    findings,
                    "MANIFEST_NOT_ISSUED",
                    "No currently issued manifest exists "
                            + "for this movement"
            );

            return;
        }

        if (issuedManifests.size() > 1) {

            addBlocker(
                    findings,
                    "MANIFEST_ISSUANCE_CONFLICT",
                    "Multiple currently issued manifests exist "
                            + "for this movement"
            );

            return;
        }

        MovementManifest issuedManifest =
                issuedManifests.getFirst();

        ManifestFreshnessAssessment assessment =
                manifestService.assessFreshness(
                        issuedManifest.id()
                );

        switch (assessment.status()) {

            case CURRENT -> {

                if (!verificationRepository.existsForCurrentOperationalRevision(
                        issuedManifest.id()
                )) {

                    addBlocker(
                            findings,
                            "QUALITY_VERIFICATION_REQUIRED",
                            "Issued manifest version "
                                    + issuedManifest.version()
                                    + " requires completed quality verification "
                                    + "for the current operational revision"
                    );
                }
            }

            case STALE -> addBlocker(
                    findings,
                    "MANIFEST_STALE",
                    "Issued manifest version "
                            + issuedManifest.version()
                            + " no longer reflects current "
                            + "operational data. Changes: "
                            + assessment.changes()
            );

            case UNVERIFIABLE -> addBlocker(
                    findings,
                    "MANIFEST_UNVERIFIABLE",
                    "Freshness of issued manifest version "
                            + issuedManifest.version()
                            + " cannot be established. Reason: "
                            + assessment.reason()
            );
        }
    }

    private void addBlocker(
            List<DispatchAssessment.Finding> findings,
            String code,
            String description
    ) {

        findings.add(
                new DispatchAssessment.Finding(
                        code,
                        DispatchAssessment.Severity.BLOCKER,
                        description
                )
        );
    }
}