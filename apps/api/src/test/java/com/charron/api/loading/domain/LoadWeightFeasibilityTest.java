
package com.charron.api.loading.domain;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoLine;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Weight;
import com.charron.api.fleet.domain.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LoadWeightFeasibilityTest {

    private Load createLoad(long weightGrams) {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Test Freight",
                null,
                null
        );

        cargo.setMeasurements(
                new CargoMeasurements(
                        new Weight(weightGrams),
                        null
                )
        );

        Load load = new Load(UUID.randomUUID());
        load.addCargoLine(new CargoLine(cargo, 1));

        return load;
    }

    @Test
    void loadWithinCapacityIsFeasible() {
        Load load = createLoad(1_900_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                new Weight(2_500_000)
        );

        assertEquals(
                LoadWeightFeasibility.Status.FEASIBLE,
                result.status()
        );

        assertEquals(
                600_000,
                result.remainingPayload().grams()
        );
    }

    @Test
    void loadExceedingCapacityIsRejected() {
        Load load = createLoad(3_000_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                new Weight(2_500_000)
        );

        assertEquals(
                LoadWeightFeasibility.Status.EXCEEDS_CAPACITY,
                result.status()
        );

        assertNull(result.remainingPayload());
    }

    @Test
    void exactCapacityIsFeasible() {
        Load load = createLoad(2_500_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                new Weight(2_500_000)
        );

        assertEquals(
                LoadWeightFeasibility.Status.FEASIBLE,
                result.status()
        );

        assertEquals(
                0,
                result.remainingPayload().grams()
        );
    }

    @Test
    void unknownCargoWeightIsIndeterminate() {
        Cargo cargo = new Cargo(
                UUID.randomUUID(),
                "Unknown Freight",
                null,
                null
        );

        Load load = new Load(UUID.randomUUID());
        load.addCargoLine(new CargoLine(cargo, 1));

        var result = LoadWeightFeasibility.evaluate(
                load,
                new Weight(2_500_000)
        );

        assertEquals(
                LoadWeightFeasibility.Status.INDETERMINATE,
                result.status()
        );

        assertNull(result.loadWeight());
        assertNull(result.remainingPayload());
    }

    @Test
    void unknownEquipmentCapacityIsIndeterminate() {
        Load load = createLoad(1_900_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                (Weight) null
        );

        assertEquals(
                LoadWeightFeasibility.Status.INDETERMINATE,
                result.status()
        );

        assertEquals(
                1_900_000,
                result.loadWeight().grams()
        );
    }

    @Test
    void evaluatesLoadAgainstEquipmentSpecifications() {
        Equipment trailer = new Equipment(
                UUID.randomUUID(),
                "Test Trailer",
                EquipmentCategory.TRAILER,
                null
        );

        trailer.updateSpecifications(
                new EquipmentSpecifications(
                        null,
                        new WeightSpecifications(
                                new Weight(2_000_000),
                                new Weight(5_000_000),
                                new Weight(2_500_000)
                        )
                )
        );

        Load load = createLoad(1_900_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                trailer,
                null
        );

        assertEquals(
                LoadWeightFeasibility.Status.FEASIBLE,
                result.status()
        );

        assertEquals(
                2_500_000,
                result.availablePayload().grams()
        );

        assertEquals(
                600_000,
                result.remainingPayload().grams()
        );
    }

    @Test
    void rejectsLoadExceedingEquipmentPayload() {
        Equipment trailer = new Equipment(
                UUID.randomUUID(),
                "Test Trailer",
                EquipmentCategory.TRAILER,
                null
        );

        trailer.updateSpecifications(
                new EquipmentSpecifications(
                        null,
                        new WeightSpecifications(
                                new Weight(2_000_000),
                                new Weight(5_000_000),
                                new Weight(2_500_000)
                        )
                )
        );

        Load load = createLoad(2_800_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                trailer,
                null
        );

        assertEquals(
                LoadWeightFeasibility.Status.EXCEEDS_CAPACITY,
                result.status()
        );
    }

    @Test
    void missingEquipmentSpecificationsAreIndeterminate() {
        Equipment trailer = new Equipment(
                UUID.randomUUID(),
                "Unspecified Trailer",
                EquipmentCategory.TRAILER,
                null
        );

        Load load = createLoad(1_900_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                trailer,
                null
        );

        assertEquals(
                LoadWeightFeasibility.Status.INDETERMINATE,
                result.status()
        );

        assertNull(result.availablePayload());
    }


    @Test
    void accountsForConfigurationWeightAdjustment() {
        UUID equipmentId = UUID.randomUUID();

        Equipment trailer = new Equipment(
                equipmentId,
                "Configured Test Trailer",
                EquipmentCategory.TRAILER,
                null
        );

        trailer.updateSpecifications(
                new EquipmentSpecifications(
                        null,
                        new WeightSpecifications(
                                new Weight(2_000_000),
                                new Weight(5_000_000),
                                new Weight(2_500_000)
                        )
                )
        );

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        UUID.randomUUID(),
                        equipmentId,
                        "Added Equipment",
                        true
                );

        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(300_000)
        );

        Load load = createLoad(1_900_000);

        var result = LoadWeightFeasibility.evaluate(
                load,
                trailer,
                configuration
        );

        assertEquals(
                LoadWeightFeasibility.Status.FEASIBLE,
                result.status()
        );

        assertEquals(
                1_900_000L,
                result.loadWeight().grams()
        );

        assertEquals(
                2_200_000L,
                result.availablePayload().grams()
        );

        assertEquals(
                300_000L,
                result.remainingPayload().grams()
        );
    }


}
