
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;

import java.util.Objects;

public record ConfigurationWeightAdjustment(
        long grams
) {

    public Weight applyTo(Weight baseWeight) {
        Objects.requireNonNull(
                baseWeight,
                "Base weight is required"
        );

        long adjustedGrams = Math.addExact(
                baseWeight.grams(),
                grams
        );

        if (adjustedGrams < 0) {
            throw new IllegalArgumentException(
                    "Adjusted weight cannot be negative"
            );
        }

        return new Weight(adjustedGrams);
    }
}
