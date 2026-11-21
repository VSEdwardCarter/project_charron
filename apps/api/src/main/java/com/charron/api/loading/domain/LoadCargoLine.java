
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoLine;

import java.util.Objects;
import java.util.UUID;

public record LoadCargoLine(
        UUID id,
        CargoLine cargoLine
) {

    public LoadCargoLine {
        Objects.requireNonNull(id, "Load cargo line ID is required");
        Objects.requireNonNull(
                cargoLine,
                "Cargo line is required"
        );
    }

    public UUID cargoId() {
        return cargoLine.cargo().getId();
    }

    public long quantity() {
        return cargoLine.quantity();
    }
}
