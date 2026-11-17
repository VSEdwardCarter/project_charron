
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationWeightAdjustmentTest {

    @Test
    void appliesPositiveAdjustment() {
        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(400_000);

        Weight result = adjustment.applyTo(
                new Weight(6_000_000)
        );

        assertEquals(
                new Weight(6_400_000),
                result
        );
    }

    @Test
    void appliesNegativeAdjustment() {
        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(-150_000);

        Weight result = adjustment.applyTo(
                new Weight(6_000_000)
        );

        assertEquals(
                new Weight(5_850_000),
                result
        );
    }

    @Test
    void allowsZeroAdjustment() {
        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(0);

        assertEquals(
                new Weight(6_000_000),
                adjustment.applyTo(new Weight(6_000_000))
        );
    }

    @Test
    void rejectsNegativeResult() {
        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(-2_000);

        assertThrows(
                IllegalArgumentException.class,
                () -> adjustment.applyTo(new Weight(1_000))
        );
    }

    @Test
    void allowsAdjustedWeightOfZero() {
        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(-1_000);

        assertEquals(
                new Weight(0),
                adjustment.applyTo(new Weight(1_000))
        );
    }

    @Test
    void rejectsNullBaseWeight() {
        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(100);

        assertThrows(
                NullPointerException.class,
                () -> adjustment.applyTo(null)
        );
    }

    @Test
    void detectsArithmeticOverflow() {
        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(1);

        assertThrows(
                ArithmeticException.class,
                () -> adjustment.applyTo(
                        new Weight(Long.MAX_VALUE)
                )
        );
    }

    @Test
    void doesNotModifyOriginalWeight() {
        Weight original = new Weight(6_000_000);

        ConfigurationWeightAdjustment adjustment =
                new ConfigurationWeightAdjustment(400_000);

        adjustment.applyTo(original);

        assertEquals(
                new Weight(6_000_000),
                original
        );
    }

    @Test
    void supportsValueEquality() {
        assertEquals(
                new ConfigurationWeightAdjustment(400_000),
                new ConfigurationWeightAdjustment(400_000)
        );
    }
}
