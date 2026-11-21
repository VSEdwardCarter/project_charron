
package com.charron.api.cargo.domain;

import java.util.Objects;
import java.util.UUID;

public class Cargo {

    private final UUID id;
    private String name;
    private String reference;
    private String description;
    private CargoMeasurements measurements;

    public Cargo(
            UUID id,
            String name,
            String reference,
            String description
    ) {
        this.id = Objects.requireNonNull(
                id,
                "Cargo ID is required"
        );

        setName(name);
        setReference(reference);
        setDescription(description);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Cargo name is required"
            );
        }

        this.name = name.trim();
    }

    public void setReference(String reference) {
        this.reference = normalizeOptional(reference);
    }

    public void setDescription(String description) {
        this.description = normalizeOptional(description);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    public CargoMeasurements getMeasurements() {
        return measurements;
    }

    public void setMeasurements(CargoMeasurements measurements) {
        this.measurements = measurements;
    }


}
