
package com.charron.api.cargo.infrastructure;

import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JdbcCargoRepositoryTest {

    @Autowired
    private JdbcCargoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Cargo createCargo(String name) {
        return new Cargo(
                UUID.randomUUID(),
                name,
                "TEST-" + UUID.randomUUID(),
                "Cargo repository integration test"
        );
    }

    private void deleteCargo(Cargo cargo) {
        jdbcTemplate.update(
                "DELETE FROM cargo WHERE id = ?",
                cargo.getId()
        );
    }

    @Test
    void insertsAndRetrievesCargoWithCompleteMeasurements() {
        Cargo cargo = createCargo("Test Generator");

        cargo.setMeasurements(
                new CargoMeasurements(
                        new Weight(150000),
                        new Dimensions(1200, 800, 1000)
                )
        );

        try {
            repository.insert(cargo);

            Cargo restored = repository.findById(cargo.getId())
                    .orElseThrow();

            assertEquals(cargo.getId(), restored.getId());
            assertEquals("Test Generator", restored.getName());
            assertEquals(cargo.getReference(), restored.getReference());
            assertEquals(cargo.getDescription(), restored.getDescription());
            assertEquals(150000, restored.getMeasurements().weight().grams());
            assertEquals(
                    new Dimensions(1200, 800, 1000),
                    restored.getMeasurements().dimensions()
            );
        } finally {
            deleteCargo(cargo);
        }
    }

    @Test
    void insertsCargoWithoutMeasurements() {
        Cargo cargo = createCargo("Unmeasured Cargo");

        try {
            repository.insert(cargo);

            Cargo restored = repository.findById(cargo.getId())
                    .orElseThrow();

            assertNull(restored.getMeasurements());
        } finally {
            deleteCargo(cargo);
        }
    }

    @Test
    void insertsCargoWithWeightOnly() {
        Cargo cargo = createCargo("Weight Only");

        cargo.setMeasurements(
                new CargoMeasurements(new Weight(25000), null)
        );

        try {
            repository.insert(cargo);

            Cargo restored = repository.findById(cargo.getId())
                    .orElseThrow();

            assertEquals(25000, restored.getMeasurements().weight().grams());
            assertNull(restored.getMeasurements().dimensions());
        } finally {
            deleteCargo(cargo);
        }
    }

    @Test
    void insertsCargoWithDimensionsOnly() {
        Cargo cargo = createCargo("Dimensions Only");

        cargo.setMeasurements(
                new CargoMeasurements(
                        null,
                        new Dimensions(600, 600, 600)
                )
        );

        try {
            repository.insert(cargo);

            Cargo restored = repository.findById(cargo.getId())
                    .orElseThrow();

            assertNull(restored.getMeasurements().weight());
            assertEquals(
                    new Dimensions(600, 600, 600),
                    restored.getMeasurements().dimensions()
            );
        } finally {
            deleteCargo(cargo);
        }
    }

    @Test
    void returnsEmptyForUnknownCargoId() {
        assertTrue(
                repository.findById(UUID.randomUUID()).isEmpty()
        );
    }

    @Test
    void findAllIncludesRegisteredCargo() {
        Cargo cargo = createCargo("Catalog Test Cargo");

        try {
            repository.insert(cargo);

            assertTrue(
                    repository.findAll().stream()
                            .anyMatch(item ->
                                    item.getId().equals(cargo.getId())
                            )
            );
        } finally {
            deleteCargo(cargo);
        }
    }

    @Test
    void rejectsZeroWeight() {
        Cargo cargo = createCargo("Invalid Weight");

        cargo.setMeasurements(
                new CargoMeasurements(new Weight(0), null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.insert(cargo)
        );

        assertTrue(repository.findById(cargo.getId()).isEmpty());
    }
}
