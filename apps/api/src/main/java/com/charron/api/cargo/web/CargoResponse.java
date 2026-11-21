
package com.charron.api.cargo.web;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoMeasurements;

import java.util.UUID;

public record CargoResponse(
        UUID id,
        String name,
        String reference,
        String description,
        Long weightGrams,
        Long lengthMm,
        Long widthMm,
        Long heightMm
) {

    public static CargoResponse from(Cargo cargo) {
        CargoMeasurements measurements = cargo.getMeasurements();

        Long weightGrams = null;
        Long lengthMm = null;
        Long widthMm = null;
        Long heightMm = null;

        if (measurements != null) {
            if (measurements.weight() != null) {
                weightGrams = measurements.weight().grams();
            }

            if (measurements.dimensions() != null) {
                lengthMm = measurements.dimensions().lengthMm();
                widthMm = measurements.dimensions().widthMm();
                heightMm = measurements.dimensions().heightMm();
            }
        }

        return new CargoResponse(
                cargo.getId(),
                cargo.getName(),
                cargo.getReference(),
                cargo.getDescription(),
                weightGrams,
                lengthMm,
                widthMm,
                heightMm
        );
    }
}
