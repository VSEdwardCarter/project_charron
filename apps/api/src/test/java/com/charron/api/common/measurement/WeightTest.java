
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class WeightTest {

    @Test
    void createsValidWeight() {
        Weight weight = new Weight(1500);

        assertEquals(1500, weight.grams());
    }

    @Test
    void allowsZeroWeight() {
        Weight weight = new Weight(0);

        assertEquals(0, weight.grams());
    }

    @Test
    void rejectsNegativeWeight() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Weight(-1)
        );
    }

    @Test
    void convertsKilogramsToGrams() {
        Weight weight = Weight.ofKilograms(
                new BigDecimal("12.5")
        );

        assertEquals(12500, weight.grams());
        assertEquals(
                new BigDecimal("12.5"),
                weight.toKilograms()
        );
    }

    @Test
    void convertsPoundsToGrams() {
        Weight weight = Weight.ofPounds(
                new BigDecimal("100")
        );

        assertEquals(45359, weight.grams());
    }

    @Test
    void addsWeights() {
        Weight first = new Weight(1000);
        Weight second = new Weight(2500);

        assertEquals(
                new Weight(3500),
                first.add(second)
        );
    }

    @Test
    void subtractsWeights() {
        Weight first = new Weight(5000);
        Weight second = new Weight(2000);

        assertEquals(
                new Weight(3000),
                first.subtract(second)
        );
    }

    @Test
    void rejectsSubtractionBelowZero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Weight(1000)
                        .subtract(new Weight(2000))
        );
    }

    @Test
    void detectsArithmeticOverflow() {
        Weight weight = new Weight(Long.MAX_VALUE);

        assertThrows(
                ArithmeticException.class,
                () -> weight.add(new Weight(1))
        );
    }

    @Test
    void supportsValueEquality() {
        assertEquals(
                new Weight(1500),
                new Weight(1500)
        );
    }
}
