
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoLine;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadCargoLineTest {

    @Test
    void identifiesCargoLineIndependentlyOfCargo() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Generator",
                null,
                null
        );

        CargoLine line = new CargoLine(cargo, 3);

        LoadCargoLine first = new LoadCargoLine(
                UUID.randomUUID(),
                line
        );

        LoadCargoLine second = new LoadCargoLine(
                UUID.randomUUID(),
                line
        );

        assertNotEquals(first.id(), second.id());
        assertEquals(first.cargoId(), second.cargoId());

        assertEquals(3, first.quantity());
        assertEquals(3, second.quantity());
    }

    @Test
    void rejectsMissingIdentity() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Generator",
                null,
                null
        );

        assertThrows(
                NullPointerException.class,
                () -> new LoadCargoLine(
                        null,
                        new CargoLine(cargo, 1)
                )
        );
    }

    @Test
    void rejectsMissingCargoLine() {
        assertThrows(
                NullPointerException.class,
                () -> new LoadCargoLine(
                        UUID.randomUUID(),
                        null
                )
        );
    }
}
