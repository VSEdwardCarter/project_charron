
package com.charron.api.cargo.web;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CargoResponseTest {

    @Test
    void mapsCargoWithCompleteMeasurements() {
        UUID id = UUID.randomUUID();

        Cargo cargo = new Cargo(
                id,
                "Generator",
                "GEN-001",
                "Portable generator"
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        new Weight(150000),
                        new Dimensions(1200, 800, 1000)
                )
        );

        CargoResponse response = CargoResponse.from(cargo);

        assertEquals(id, response.id());
        assertEquals("Generator", response.name());
        assertEquals("GEN-001", response.reference());
        assertEquals("Portable generator", response.description());
        assertEquals(150000L, response.weightGrams());
        assertEquals(1200L, response.lengthMm());
        assertEquals(800L, response.widthMm());
        assertEquals(1000L, response.heightMm());
    }

    @Test
    void preservesMissingMeasurements() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Unknown Cargo",
                null,
                null
        );

        CargoResponse response = CargoResponse.from(cargo);

        assertNull(response.reference());
        assertNull(response.description());
        assertNull(response.weightGrams());
        assertNull(response.lengthMm());
        assertNull(response.widthMm());
        assertNull(response.heightMm());
    }

    @Test
    void mapsWeightOnlyCargo() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Steel Component",
                null,
                null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        new Weight(50000),
                        null
                )
        );

        CargoResponse response = CargoResponse.from(cargo);

        assertEquals(50000L, response.weightGrams());
        assertNull(response.lengthMm());
        assertNull(response.widthMm());
        assertNull(response.heightMm());
    }
}
