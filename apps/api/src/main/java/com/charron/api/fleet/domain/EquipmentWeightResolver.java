
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;

import java.util.Objects;

public final class EquipmentWeightResolver {

    private EquipmentWeightResolver() {
    }

    public static Weight resolveEmptyWeight(
            Equipment equipment,
            EquipmentConfiguration configuration
    ) {
        Objects.requireNonNull(
                equipment,
                "Equipment is required"
        );

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

        if (specifications == null
                || specifications.weightSpecifications() == null
                || specifications.weightSpecifications()
                .emptyWeight() == null) {
            throw new IllegalStateException(
                    "Base equipment empty weight is unknown"
            );
        }

        WeightSpecifications weights =
                specifications.weightSpecifications();

        Weight effectiveWeight = weights.emptyWeight();

        if (configuration != null
                && configuration.getWeightAdjustment() != null) {
            effectiveWeight = configuration
                    .getWeightAdjustment()
                    .applyTo(effectiveWeight);
        }

        if (weights.grossWeightRating() != null
                && effectiveWeight.grams()
                > weights.grossWeightRating().grams()) {
            throw new IllegalStateException(
                    "Effective empty weight exceeds gross weight rating"
            );
        }

        return effectiveWeight;
    }

    public static Weight resolveAvailablePayload(
            Equipment equipment,
            EquipmentConfiguration configuration
    ) {
        Weight effectiveEmptyWeight =
                resolveEmptyWeight(equipment, configuration);

        WeightSpecifications weights =
                equipment.getSpecifications().weightSpecifications();

        Weight baseEmptyWeight = weights.emptyWeight();
        Weight grossWeightRating = weights.grossWeightRating();
        Weight ratedPayloadCapacity = weights.ratedPayloadCapacity();

        if (grossWeightRating == null
                && ratedPayloadCapacity == null) {
            throw new IllegalStateException(
                    "Gross weight rating or rated payload capacity is required"
            );
        }

        long configurationDelta = Math.subtractExact(
                effectiveEmptyWeight.grams(),
                baseEmptyWeight.grams()
        );

        Long availableByGrossRating = grossWeightRating == null
                ? null
                : Math.subtractExact(
                grossWeightRating.grams(),
                effectiveEmptyWeight.grams()
        );

        Long availableByRatedPayload = ratedPayloadCapacity == null
                ? null
                : Math.subtractExact(
                ratedPayloadCapacity.grams(),
                configurationDelta
        );

        long availableGrams;

        if (availableByGrossRating != null
                && availableByRatedPayload != null) {
            availableGrams = Math.min(
                    availableByGrossRating,
                    availableByRatedPayload
            );
        } else if (availableByGrossRating != null) {
            availableGrams = availableByGrossRating;
        } else {
            availableGrams = availableByRatedPayload;
        }

        return new Weight(Math.max(0L, availableGrams));
    }

}
