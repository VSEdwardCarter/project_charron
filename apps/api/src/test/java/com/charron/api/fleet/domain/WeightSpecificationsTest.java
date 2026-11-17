
package com.charron.api.fleet.domain;

import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeightSpecificationsTest {

    @Test
    void createsValidWeightSpecifications() {
        WeightSpecifications specifications =
                new WeightSpecifications(
                        new Weight(7_000_000),
                        new Weight(36_000_000)
                );

        assertEquals(
                new Weight(7_000_000),
                specifications.emptyWeight()
        );

        assertEquals(
                new Weight(36_000_000),
                specifications.grossWeightRating()
        );
    }

    @Test
    void allowsUnknownWeights() {
        WeightSpecifications specifications =
                new WeightSpecifications(null, null);

        assertNull(specifications.emptyWeight());
        assertNull(specifications.grossWeightRating());
    }

    @Test
    void allowsOnlyEmptyWeight() {
        WeightSpecifications specifications =
                new WeightSpecifications(
                        new Weight(7_000_000),
                        null
                );

        assertNull(specifications.grossWeightRating());
    }

    @Test
    void allowsOnlyGrossWeightRating() {
        WeightSpecifications specifications =
                new WeightSpecifications(
                        null,
                        new Weight(36_000_000)
                );

        assertNull(specifications.emptyWeight());
    }

    @Test
    void rejectsEmptyWeightGreaterThanGrossRating() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeightSpecifications(
                        new Weight(40_000_000),
                        new Weight(36_000_000)
                )
        );
    }

    @Test
    void rejectsZeroGrossWeightRating() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WeightSpecifications(
                        new Weight(0),
                        new Weight(0)
                )
        );
    }

    @Test
    void allowsEqualEmptyAndGrossWeights() {
        Weight weight = new Weight(10_000_000);

        assertDoesNotThrow(
                () -> new WeightSpecifications(weight, weight)
        );
    }

    @Test
    void supportsValueEquality() {
        WeightSpecifications first =
                new WeightSpecifications(
                        new Weight(7_000_000),
                        new Weight(36_000_000)
                );

        WeightSpecifications second =
                new WeightSpecifications(
                        new Weight(7_000_000),
                        new Weight(36_000_000)
                );

        assertEquals(first, second);
    }
}
