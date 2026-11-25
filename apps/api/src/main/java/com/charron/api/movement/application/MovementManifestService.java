
package com.charron.api.movement.application;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.loading.application.LoadingCargoCatalogLookup;
import com.charron.api.loading.application.LoadingPlanArrangementRepository;
import com.charron.api.loading.application.LoadingPlanCargoRepository;
import com.charron.api.loading.application.LoadingPlanService;
import com.charron.api.loading.domain.LoadingPlan;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import com.charron.api.loading.domain.LoadingPlanCargo;
import com.charron.api.movement.domain.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MovementManifestService {

    private final MovementRepository movementRepository;
    private final MovementManifestRepository manifestRepository;
    private final MovementEquipmentAssignmentRepository
            assignmentRepository;
    private final EquipmentRepository equipmentRepository;
    private final LoadingPlanService loadingPlanService;
    private final LoadingPlanCargoRepository cargoRepository;
    private final LoadingCargoCatalogLookup cargoCatalog;
    private final LoadingPlanArrangementRepository
            arrangementRepository;

    public MovementManifestService(
            MovementRepository movementRepository,
            MovementManifestRepository manifestRepository,
            MovementEquipmentAssignmentRepository assignmentRepository,
            EquipmentRepository equipmentRepository,
            LoadingPlanService loadingPlanService,
            LoadingPlanCargoRepository cargoRepository,
            LoadingCargoCatalogLookup cargoCatalog,
            LoadingPlanArrangementRepository arrangementRepository
    ) {
        this.movementRepository =
                Objects.requireNonNull(movementRepository);
        this.manifestRepository =
                Objects.requireNonNull(manifestRepository);
        this.assignmentRepository =
                Objects.requireNonNull(assignmentRepository);
        this.equipmentRepository =
                Objects.requireNonNull(equipmentRepository);
        this.loadingPlanService =
                Objects.requireNonNull(loadingPlanService);
        this.cargoRepository =
                Objects.requireNonNull(cargoRepository);
        this.cargoCatalog =
                Objects.requireNonNull(cargoCatalog);
        this.arrangementRepository =
                Objects.requireNonNull(arrangementRepository);
    }

    @Transactional(readOnly = true)
    public MovementManifest generateDraft(UUID movementId) {
        Objects.requireNonNull(
                movementId,
                "Movement ID is required"
        );

        Movement movement = movementRepository
                .findById(movementId)
                .orElseThrow(
                        () -> new MovementNotFoundException(movementId)
                );

        return buildManifest(movement, 1);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovementManifest generateAndPersist(UUID movementId) {

        Objects.requireNonNull(
                movementId,
                "Movement ID is required"
        );

        Movement movement = movementRepository
                .findByIdForUpdate(movementId)
                .orElseThrow(
                        () -> new MovementNotFoundException(movementId)
                );

        int version = Math.incrementExact(
                manifestRepository.findLatestVersion(movementId)
        );

        MovementManifest manifest =
                buildManifest(movement, version);

        manifestRepository.save(manifest);

        return manifest;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public ManifestFreshnessAssessment assessFreshness(
            UUID manifestId
    ) {

        Objects.requireNonNull(
                manifestId,
                "Manifest ID is required"
        );

        MovementManifest persisted = manifestRepository
                .findById(manifestId)
                .orElseThrow(
                        () -> new ManifestNotFoundException(manifestId)
                );

        var movement = movementRepository.findById(
                persisted.movementId()
        );

        if (movement.isEmpty()) {

            return unverifiable(
                    persisted.id(),
                    "Associated movement could not be found"
            );
        }

        MovementManifest current;

        try {

            current = buildManifest(
                    movement.get(),
                    persisted.version()
            );

        } catch (ManifestSnapshotUnavailableException exception) {

            return unverifiable(
                    persisted.id(),
                    exception.getMessage()
            );
        }

        return ManifestFreshnessComparator.compare(
                persisted,
                current
        );
    }

    private ManifestFreshnessAssessment unverifiable(
            UUID manifestId,
            String reason
    ) {

        return new ManifestFreshnessAssessment(
                manifestId,
                ManifestFreshnessStatus.UNVERIFIABLE,
                Set.of(),
                Instant.now().truncatedTo(ChronoUnit.MICROS),
                reason
        );
    }



    private MovementManifest buildManifest(
            Movement movement,
            int version
    ) {

        UUID movementId = movement.id();

        List<MovementManifest.EquipmentEntry> equipment =
                new ArrayList<>();

        List<MovementEquipmentAssignment> assignments =
                assignmentRepository.findByMovementId(movementId);

        for (MovementEquipmentAssignment assignment : assignments) {
            Equipment item = equipmentRepository
                    .findIdentityById(assignment.equipmentId())
                    .orElseThrow(
                            () -> new ManifestSnapshotUnavailableException(
                                    "Assigned equipment not found: "
                                            + assignment.equipmentId()
                            )
                    );
            equipment.add(
                    new MovementManifest.EquipmentEntry(
                            item.getId(),
                            item.getName(),
                            assignment.role(),
                            item.getVin()
                    )
            );
        }

        List<MovementManifest.CargoEntry> cargo =
                new ArrayList<>();

        List<MovementManifest.LoadingEntry> loading =
                new ArrayList<>();

        List<LoadingPlan> plans =
                loadingPlanService.getByMovementId(movementId);

        for (LoadingPlan plan : plans) {
            List<LoadingPlanCargo> cargoLines =
                    cargoRepository.findByLoadingPlanId(plan.id());

            for (LoadingPlanCargo line : cargoLines) {
                var item = cargoCatalog
                        .findById(line.cargoId())
                        .orElseThrow(
                                () -> new ManifestSnapshotUnavailableException(
                                        "Cargo catalog entry not found: "
                                                + line.cargoId()
                                )
                        );

                cargo.add(
                        new MovementManifest.CargoEntry(
                                plan.id(),
                                item.id(),
                                item.name(),
                                line.quantity(),
                                item.lengthMm(),
                                item.widthMm(),
                                item.heightMm(),
                                item.weightGrams()
                        )
                );
            }

            LoadingPlanArrangement arrangement =
                    arrangementRepository
                            .findByLoadingPlanId(plan.id())
                            .orElseThrow(
                                    () -> new ManifestSnapshotUnavailableException(
                                            "No saved loading arrangement for plan: "
                                                    + plan.id()
                                    )
                            );

            loading.add(
                    new MovementManifest.LoadingEntry(
                            plan.id(),
                            plan.equipmentId(),
                            plan.cargoSpaceId(),
                            arrangement.status(),
                            arrangement.previewSnapshot()
                    )
            );
        }

        return new MovementManifest(
                UUID.randomUUID(),
                movement.id(),
                version,
                movement.reference(),
                movement.origin(),
                movement.destination(),
                movement.plannedDeparture(),
                movement.plannedArrival(),
                equipment,
                cargo,
                loading,
                Instant.now().truncatedTo(ChronoUnit.MICROS)
        );
    }

}
