package com.charron.api.fleet.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionsTest {

    @Test
    void createsValidDimensions() {
        Dimensions dimensions = new Dimensions(
                12000,
                2500,
                2600
        );

        assertEquals(12000, dimensions.lengthMm());
        assertEquals(2500, dimensions.widthMm());
        assertEquals(2600, dimensions.heightMm());
    }

    @Test
    void calculatesVolume() {
        Dimensions dimensions = new Dimensions(
                2000,
                1000,
                1000
        );

        assertEquals(
                2_000_000_000L,
                dimensions.volumeCubicMm()
        );
    }

    @Test
    void rejectsZeroDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Dimensions(0, 1000, 1000)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Dimensions(1000, 0, 1000)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Dimensions(1000, 1000, 0)
        );
    }

    @Test
    void rejectsNegativeDimensions() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Dimensions(-100, 1000, 1000)
        );
    }

    @Test
    void supportsValueEquality() {
        Dimensions first = new Dimensions(
                1000, 2000, 3000
        );

        Dimensions second = new Dimensions(
                1000, 2000, 3000
        );

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void detectsVolumeOverflow() {
        Dimensions dimensions = new Dimensions(
                Long.MAX_VALUE,
                2,
                2
        );

        assertThrows(
                ArithmeticException.class,
                dimensions::volumeCubicMm
        );
    }
}



