package com.charron.api.fleet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Weight(long grams) {

    private static final BigDecimal GRAMS_PER_POUND =
            new BigDecimal("453.59237");

    public Weight {
        if (grams < 0) {
            throw new IllegalArgumentException(
                    "Weight cannot be negative"
            );
        }
    }

    public static Weight ofKilograms(BigDecimal kilograms) {
        Objects.requireNonNull(kilograms, "Kilograms are required");

        long grams = kilograms
                .multiply(BigDecimal.valueOf(1000))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        return new Weight(grams);
    }

    public static Weight ofPounds(BigDecimal pounds) {
        Objects.requireNonNull(pounds, "Pounds are required");

        long grams = pounds
                .multiply(GRAMS_PER_POUND)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();

        return new Weight(grams);
    }

    public BigDecimal toKilograms() {
        return BigDecimal.valueOf(grams)
                .divide(BigDecimal.valueOf(1000));
    }

    public BigDecimal toPounds() {
        return BigDecimal.valueOf(grams)
                .divide(GRAMS_PER_POUND, 6, RoundingMode.HALF_UP);
    }

    public Weight add(Weight other) {
        Objects.requireNonNull(other, "Other weight is required");

        return new Weight(
                Math.addExact(grams, other.grams)
        );
    }

    public Weight subtract(Weight other) {
        Objects.requireNonNull(other, "Other weight is required");

        return new Weight(
                Math.subtractExact(grams, other.grams)
        );
    }
}
