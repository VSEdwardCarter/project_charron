package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Dimensions;

import java.util.Objects;

public record EquipmentSpecifications(
        Dimensions exteriorDimensions
) {

    public EquipmentSpecifications {
        Objects.requireNonNull(
                exteriorDimensions,
                "Exterior dimensions are required"
        );
    }
}