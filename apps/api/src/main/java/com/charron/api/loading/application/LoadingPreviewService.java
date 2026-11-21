
package com.charron.api.loading.application;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import com.charron.api.fleet.application.EquipmentCargoSpaceRepository;
import com.charron.api.loading.domain.LoadCargoLine;
import com.charron.api.loading.domain.LoadCargoPlacement;
import com.charron.api.loading.domain.LoadPlanValidator;
import com.charron.api.loading.domain.MixedCargoPlacementGenerator;
import com.charron.api.loading.web.LoadingPreviewRequest;
import com.charron.api.loading.web.LoadingPreviewResponse;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoadingPreviewService {

    private final EquipmentCargoSpaceRepository cargoSpaceRepository;

    public LoadingPreviewService(
            EquipmentCargoSpaceRepository cargoSpaceRepository
    ) {
        this.cargoSpaceRepository =
                Objects.requireNonNull(cargoSpaceRepository);
    }

    public LoadingPreviewResponse preview(
            LoadingPreviewRequest request
    ) {
        Objects.requireNonNull(request, "Request is required");

        if (request.cargoSpaceId() == null) {
            return failure(null, "INVALID", "Cargo space ID is required");
        }

        if (request.cargo() == null || request.cargo().isEmpty()) {
            return failure(
                    request.cargoSpaceId(),
                    "INVALID",
                    "At least one cargo item is required"
            );
        }

        var storedSpace = cargoSpaceRepository
                .findById(request.cargoSpaceId());

        if (storedSpace.isEmpty()) {
            return failure(
                    request.cargoSpaceId(),
                    "INVALID",
                    "Cargo space was not found"
            );
        }

        var space = storedSpace.orElseThrow();

        List<LoadCargoLine> lines = new ArrayList<>();
        Map<UUID, LoadingPreviewRequest.CargoItem> itemsByLine =
                new HashMap<>();

        for (var item : request.cargo()) {
            if (item == null) {
                return failure(
                        request.cargoSpaceId(),
                        "INVALID",
                        "Cargo item cannot be null"
                );
            }

            if (item.cargoId() == null
                    || item.name() == null
                    || item.name().isBlank()
                    || item.quantity() <= 0) {
                return failure(
                        request.cargoSpaceId(),
                        "INVALID",
                        "Cargo identity, name, and positive quantity are required"
                );
            }

            if (item.lengthMm() <= 0
                    || item.widthMm() <= 0
                    || item.heightMm() <= 0) {
                return failure(
                        request.cargoSpaceId(),
                        "INDETERMINATE",
                        "Positive cargo dimensions are required"
                );
            }

            if (item.weightGrams() != null
                    && item.weightGrams() < 0) {
                return failure(
                        request.cargoSpaceId(),
                        "INVALID",
                        "Cargo weight cannot be negative"
                );
            }

            Cargo cargo = new Cargo(
                    item.cargoId(),
                    item.name(),
                    null,
                    null
            );

            cargo.setMeasurements(
                    new CargoMeasurements(
                            item.weightGrams() == null
                                    ? null
                                    : new Weight(item.weightGrams()),
                            new Dimensions(
                                    item.lengthMm(),
                                    item.widthMm(),
                                    item.heightMm()
                            )
                    )
            );

            UUID lineId = UUID.randomUUID();

            lines.add(
                    new LoadCargoLine(
                            lineId,
                            new CargoLine(cargo, item.quantity())
                    )
            );

            itemsByLine.put(lineId, item);
        }

        List<LoadCargoPlacement> placements;

        try {
            placements = MixedCargoPlacementGenerator.generate(
                    lines,
                    space.id(),
                    space.specifications()
            );
        } catch (IllegalStateException exception) {
            return failure(
                    request.cargoSpaceId(),
                    "NOT_PLACED",
                    exception.getMessage()
            );
        }

        var validation = LoadPlanValidator.validate(
                lines,
                placements,
                Map.of(space.id(), space.specifications())
        );

        if (validation.status() != LoadPlanValidator.Status.VALID) {
            return failure(
                    request.cargoSpaceId(),
                    validation.status().name(),
                    validation.reason()
            );
        }

        List<LoadingPreviewResponse.CargoPlacementResponse> responsePlacements =
                new ArrayList<>();

        for (LoadCargoPlacement loadPlacement : placements) {
            var position = loadPlacement.placement();
            var item = itemsByLine.get(loadPlacement.loadCargoLineId());

            responsePlacements.add(
                    new LoadingPreviewResponse.CargoPlacementResponse(
                            loadPlacement.loadCargoLineId(),
                            position.cargoId(),
                            position.cargoSpaceId(),
                            position.unitIndex(),
                            position.xMm(),
                            position.yMm(),
                            position.zMm(),
                            position.orientation().name(),
                            item.lengthMm(),
                            item.widthMm(),
                            item.heightMm(),
                            item.name()
                    )
            );
        }

        return new LoadingPreviewResponse(
                space.id(),
                "VALID",
                validation.reason(),
                List.copyOf(responsePlacements)
        );
    }

    private LoadingPreviewResponse failure(
            UUID cargoSpaceId,
            String status,
            String reason
    ) {
        return new LoadingPreviewResponse(
                cargoSpaceId,
                status,
                reason,
                List.of()
        );
    }
}
