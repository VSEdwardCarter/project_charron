package com.charron.api.fleet.domain;
import java.util.Objects;
import java.util.UUID;

public class Equipment {

    private final UUID id;
    private String name;
    private EquipmentCategory category;
    private String vin;

    public Equipment(
            UUID id,
            String name,
            EquipmentCategory category,
            String vin
    ) {
        this.id = Objects.requireNonNull(
                id, "Equipment ID is required"
        );

        setName(name);

        this.category = Objects.requireNonNull(
                category, "Equipment category is required"
        );

        this.vin = normalizeVin(vin);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EquipmentCategory getCategory() {
        return category;
    }

    public String getVin() {
        return vin;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Equipment name is required"
            );
        }

        this.name = name.trim();
    }

    private String normalizeVin(String vin) {
        if (vin == null || vin.isBlank()) {
            return null;
        }

        return vin.trim().toUpperCase(
                java.util.Locale.ROOT
        );
    }
}