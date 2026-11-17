
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;

public record WeightSpecifications(
        Weight emptyWeight,
        Weight grossWeightRating,
        Weight ratedPayloadCapacity
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

        if (ratedPayloadCapacity != null
                && emptyWeight != null
                && grossWeightRating != null) {

            long maximumPayloadGrams =
                    grossWeightRating.grams()
                            - emptyWeight.grams();

            if (ratedPayloadCapacity.grams()
                    > maximumPayloadGrams) {
                throw new IllegalArgumentException(
                        "Rated payload exceeds gross weight allowance"
                );
            }
        }
    }

    // Preserve existing two-argument construction
    public WeightSpecifications(
            Weight emptyWeight,
            Weight grossWeightRating
    ) {
        this(emptyWeight, grossWeightRating, null);
    }
}
