
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadTest {

    private CargoLine line(
            String name,
            long unitWeightGrams,
            long quantity
    ) {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                name,
                null,
                null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        new Weight(unitWeightGrams),
                        null
                )
        );

        return new CargoLine(cargo, quantity);
    }

    @Test
    void createsLoadWithIdentity() {
        UUID id = UUID.randomUUID();

        Load load = new Load(id);

        assertEquals(id, load.getId());
        assertTrue(load.getCargoLines().isEmpty());
    }

    @Test
    void calculatesCombinedCargoWeight() {
        Load load = new Load(UUID.randomUUID());

        load.addCargoLine(
                line("Generators", 150000, 12)
        );

        load.addCargoLine(
                line("Parts", 25000, 4)
        );

        assertEquals(
                1_900_000L,
                load.totalWeight().orElseThrow().grams()
        );
    }

    @Test
    void reportsUnknownWeightWhenCargoIsUnmeasured() {
        Load load = new Load(UUID.randomUUID());

        load.addCargoLine(
                line("Known Cargo", 50000, 2)
        );

        Cargo unknown = new Cargo(
                UUID.randomUUID(),
                "Unknown Freight",
                null,
                null
        );

        load.addCargoLine(
                new CargoLine(unknown, 1)
        );

        assertTrue(load.totalWeight().isEmpty());
    }

    @Test
    void emptyLoadHasZeroWeight() {
        Load load = new Load(UUID.randomUUID());

        assertEquals(
                0L,
                load.totalWeight().orElseThrow().grams()
        );
    }

    @Test
    void rejectsMissingCargoLine() {
        Load load = new Load(UUID.randomUUID());

        assertThrows(
                NullPointerException.class,
                () -> load.addCargoLine(null)
        );
    }

    @Test
    void rejectsMissingLoadIdentity() {
        assertThrows(
                NullPointerException.class,
                () -> new Load(null)
        );
    }

    @Test
    void protectsCargoLineCollection() {
        Load load = new Load(UUID.randomUUID());

        assertThrows(
                UnsupportedOperationException.class,
                () -> load.getCargoLines().add(
                        line("Extra Cargo", 1000, 1)
                )
        );
    }
}
