
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.common.measurement.Weight;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Load {

    private final UUID id;
    private final List<CargoLine> cargoLines = new ArrayList<>();

    public Load(UUID id) {
        this.id = Objects.requireNonNull(
                id,
                "Load ID is required"
        );
    }

    public UUID getId() {
        return id;
    }

    public List<CargoLine> getCargoLines() {
        return List.copyOf(cargoLines);
    }

    public void addCargoLine(CargoLine line) {
        cargoLines.add(
                Objects.requireNonNull(
                        line,
                        "Cargo line is required"
                )
        );
    }

    public Optional<Weight> totalWeight() {
        long totalGrams = 0;

        for (CargoLine line : cargoLines) {
            Optional<Weight> lineWeight = line.totalWeight();

            if (lineWeight.isEmpty()) {
                return Optional.empty();
            }

            totalGrams = Math.addExact(
                    totalGrams,
                    lineWeight.orElseThrow().grams()
            );
        }

        return Optional.of(new Weight(totalGrams));
    }
}
