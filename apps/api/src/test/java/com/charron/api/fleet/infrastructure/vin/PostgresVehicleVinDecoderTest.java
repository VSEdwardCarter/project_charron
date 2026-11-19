
package com.charron.api.fleet.infrastructure.vin;

import com.charron.api.fleet.domain.DecodedVehicle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostgresVehicleVinDecoderTest {

    @Autowired
    private PostgresVehicleVinDecoder decoder;

    @Test
    void decodesHondaAccordWithEngineInformation() {
        DecodedVehicle vehicle =
                decoder.decode("1HGCM82633A004352");

        assertEquals("1HGCM82633A004352", vehicle.vin());
        assertEquals("0", vehicle.errorCode());

        assertEquals("HONDA", vehicle.make());
        assertEquals("Accord", vehicle.model());
        assertEquals(2003, vehicle.modelYear());

        assertEquals("J30A4", vehicle.engineModel());
        assertEquals("V-Shaped", vehicle.engineConfiguration());
        assertEquals(6, vehicle.engineCylinders());

        assertEquals(2.998832712, vehicle.displacementLiters(), 0.000001);
        assertEquals(240.0, vehicle.engineHorsepower(), 0.001);

        assertEquals("Gasoline", vehicle.fuelType());
        assertEquals("Automatic", vehicle.transmission());

        assertNull(vehicle.engineManufacturer());
        assertNull(vehicle.driveType());
    }


    @Test
    void preservesCompleteVinDecodeResult() {
        var rows = decoder.decodeRaw("1HGCM82633A004352");

        assertEquals(44, rows.size());

        assertTrue(rows.stream().anyMatch(row ->
                "EngineModel".equals(row.get("code"))
                        && "J30A4".equals(row.get("value"))
        ));

        assertTrue(rows.stream().anyMatch(row ->
                "EngineModel".equals(row.get("code"))
                        && row.containsKey("itempatternid")
                        && row.containsKey("itemsource")
        ));

        assertTrue(rows.stream().anyMatch(row ->
                row.get("value") == null
                        || row.get("value").toString().isBlank()
        ));
    }

}
