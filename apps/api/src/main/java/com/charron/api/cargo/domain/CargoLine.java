
package com.charron.api.cargo.domain;

import com.charron.api.common.measurement.Weight;

import java.util.Objects;
import java.util.Optional;

public record CargoLine(
        Cargo cargo,
        long quantity
) {

    public CargoLine {
        Objects.requireNonNull(cargo, "Cargo is required");

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Cargo quantity must be greater than zero"
            );
        }
    }

    public Optional<Weight> totalWeight() {
        CargoMeasurements measurements = cargo.getMeasurements();

        if (measurements == null
                || !measurements.hasWeight()) {
            return Optional.empty();
        }

        long totalGrams = Math.multiplyExact(
                measurements.weight().grams(),
                quantity
        );

        return Optional.of(new Weight(totalGrams));
    }
}
