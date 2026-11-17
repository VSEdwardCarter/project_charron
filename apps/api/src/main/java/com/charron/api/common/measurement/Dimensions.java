package com.charron.api.fleet.domain;

public record Dimensions(
        long lengthMm,
        long widthMm,
        long heightMm
) {

    public Dimensions {
        if (lengthMm <= 0) {
            throw new IllegalArgumentException(
                    "Length must be greater than zero"
            );
        }

        if (widthMm <= 0) {
            throw new IllegalArgumentException(
                    "Width must be greater than zero"
            );
        }

        if (heightMm <= 0) {
            throw new IllegalArgumentException(
                    "Height must be greater than zero"
            );
        }
    }

    public long volumeCubicMm() {
        return Math.multiplyExact(
                Math.multiplyExact(lengthMm, widthMm),
                heightMm
        );
    }
}
