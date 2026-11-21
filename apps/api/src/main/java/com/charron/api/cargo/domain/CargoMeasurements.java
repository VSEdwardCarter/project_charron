
package com.charron.api.cargo.domain;

import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;

import java.util.Objects;

public record CargoMeasurements(
        Weight weight,
        Dimensions dimensions
) {

    public CargoMeasurements {
        if (weight == null && dimensions == null) {
            throw new IllegalArgumentException(
                    "At least one cargo measurement is required"
            );
        }
    }

    public boolean hasWeight() {
        return weight != null;
    }

    public boolean hasDimensions() {
        return dimensions != null;
    }
}
