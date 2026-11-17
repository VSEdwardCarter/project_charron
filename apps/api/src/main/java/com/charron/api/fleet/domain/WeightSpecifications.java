
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;

public record WeightSpecifications(
        Weight emptyWeight,
        Weight grossWeightRating
) {

    public WeightSpecifications {
        if (grossWeightRating != null
                && grossWeightRating.grams() == 0) {
            throw new IllegalArgumentException(
                    "Gross weight rating must be greater than zero"
            );
        }

        if (emptyWeight != null
                && grossWeightRating != null
                && emptyWeight.grams() > grossWeightRating.grams()) {
            throw new IllegalArgumentException(
                    "Empty weight cannot exceed gross weight rating"
            );
        }
    }
}
