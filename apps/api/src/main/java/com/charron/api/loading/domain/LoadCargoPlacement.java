
package com.charron.api.loading.domain;

import java.util.Objects;
import java.util.UUID;

public record LoadCargoPlacement(
        UUID loadCargoLineId,
        CargoPlacement placement
) {

    public LoadCargoPlacement {
        Objects.requireNonNull(
                loadCargoLineId,
                "Load cargo line ID is required"
        );

        Objects.requireNonNull(
                placement,
                "Cargo placement is required"
        );
    }

    public UUID cargoId() {
        return placement.cargoId();
    }

    public long unitIndex() {
        return placement.unitIndex();
    }
}
