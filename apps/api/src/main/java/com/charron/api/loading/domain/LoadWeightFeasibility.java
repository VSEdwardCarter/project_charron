
package com.charron.api.loading.domain;

import com.charron.api.common.measurement.Weight;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentConfiguration;
import com.charron.api.fleet.domain.EquipmentSpecifications;
import com.charron.api.fleet.domain.EquipmentWeightResolver;
import com.charron.api.fleet.domain.WeightSpecifications;

import java.util.Objects;
import java.util.Optional;

public final class LoadWeightFeasibility {

    private LoadWeightFeasibility() {
    }

    public enum Status {
        FEASIBLE,
        EXCEEDS_CAPACITY,
        INDETERMINATE
    }

    public record Result(
            Status status,
            Weight loadWeight,
            Weight availablePayload,
            Weight remainingPayload,
            String reason
    ) {}

    public static Result evaluate(
            Load load,
            Equipment equipment,
            EquipmentConfiguration configuration
    ) {
        Objects.requireNonNull(load, "Load is required");
        Objects.requireNonNull(equipment, "Equipment is required");

        // Validate configuration ownership and active status.
        // Invalid configurations must not be treated as unknown capacity.
        if (configuration != null) {
            if (!equipment.getId().equals(
                    configuration.getEquipmentId())) {
                throw new IllegalArgumentException(
                        "Configuration does not belong to equipment"
                );
            }

            if (!configuration.isActive()) {
                throw new IllegalStateException(
                        "Configuration must be active"
                );
            }
        }

        EquipmentSpecifications specifications =
                equipment.getSpecifications();

        WeightSpecifications weights = specifications == null
                ? null
                : specifications.weightSpecifications();

        if (weights == null
                || weights.emptyWeight() == null
                || (weights.grossWeightRating() == null
                && weights.ratedPayloadCapacity() == null)) {

            return new Result(
                    Status.INDETERMINATE,
                    load.totalWeight().orElse(null),
                    null,
                    null,
                    "Equipment payload capacity is unknown"
            );
        }

        Weight availablePayload =
                EquipmentWeightResolver.resolveAvailablePayload(
                        equipment,
                        configuration
                );

        return evaluate(load, availablePayload);
    }

    public static Result evaluate(
            Load load,
            Weight availablePayload
    ) {
        Objects.requireNonNull(load, "Load is required");

        Optional<Weight> totalWeight = load.totalWeight();

        if (totalWeight.isEmpty()) {
            return new Result(
                    Status.INDETERMINATE,
                    null,
                    availablePayload,
                    null,
                    "One or more cargo lines have unknown weight"
            );
        }

        Weight loadWeight = totalWeight.orElseThrow();

        if (availablePayload == null) {
            return new Result(
                    Status.INDETERMINATE,
                    loadWeight,
                    null,
                    null,
                    "Equipment payload capacity is unknown"
            );
        }

        if (loadWeight.grams() > availablePayload.grams()) {
            return new Result(
                    Status.EXCEEDS_CAPACITY,
                    loadWeight,
                    availablePayload,
                    null,
                    "Load weight exceeds available payload"
            );
        }

        Weight remainingPayload =
                availablePayload.subtract(loadWeight);

        return new Result(
                Status.FEASIBLE,
                loadWeight,
                availablePayload,
                remainingPayload,
                "Load weight is within available payload"
        );
    }
}
