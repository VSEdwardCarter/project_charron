
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Dimensions;

public record EquipmentSpecifications(
        Dimensions exteriorDimensions,
        WeightSpecifications weightSpecifications
) {

    public EquipmentSpecifications {
        if (exteriorDimensions == null
                && weightSpecifications == null) {
            throw new IllegalArgumentException(
                    "At least one equipment specification is required"
            );
        }
    }
}
