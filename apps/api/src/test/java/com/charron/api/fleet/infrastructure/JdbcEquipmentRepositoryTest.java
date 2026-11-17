
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import com.charron.api.fleet.domain.EquipmentSpecifications;
import com.charron.api.fleet.domain.WeightSpecifications;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class JdbcEquipmentRepositoryTest {

    @Autowired
    private EquipmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsEquipmentIdentity() {
        UUID id = UUID.randomUUID();

        Equipment equipment = new Equipment(
                id,
                "CHARRON Test Tractor",
                EquipmentCategory.TRACTOR,
                null
        );

        repository.insertIdentity(equipment);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM equipment WHERE id = ?",
                Integer.class,
                id
        );

        assertEquals(1, count);
    }


    @Test
    void retrievesEquipmentIdentityById() {
        UUID id = UUID.randomUUID();

        Equipment original = new Equipment(
                id,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                "1HGCM82633A004352"
        );

        repository.insertIdentity(original);

        Equipment retrieved = repository.findIdentityById(id)
                .orElseThrow();

        assertEquals(original.getId(), retrieved.getId());
        assertEquals(original.getName(), retrieved.getName());
        assertEquals(original.getCategory(), retrieved.getCategory());
        assertEquals(original.getVin(), retrieved.getVin());
    }

    @Test
    void returnsEmptyWhenEquipmentDoesNotExist() {
        UUID nonexistentId = UUID.randomUUID();

        assertTrue(
                repository.findIdentityById(nonexistentId).isEmpty()
        );
    }

    @Test
    void retrievesEquipmentWithoutVin() {
        UUID id = UUID.randomUUID();

        Equipment original = new Equipment(
                id,
                "CHARRON Warehouse Forklift",
                EquipmentCategory.MATERIAL_HANDLING,
                null
        );

        repository.insertIdentity(original);

        Equipment retrieved = repository.findIdentityById(id)
                .orElseThrow();

        assertEquals(id, retrieved.getId());
        assertNull(retrieved.getVin());
    }


    @Test
    void insertsEquipmentSpecifications() {
        UUID id = UUID.randomUUID();

        Equipment equipment = new Equipment(
                id,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        );

        repository.insertIdentity(equipment);

        EquipmentSpecifications specifications =
                new EquipmentSpecifications(
                        new Dimensions(12000, 2500, 4000),
                        new WeightSpecifications(
                                new Weight(5000000),
                                new Weight(15000000),
                                new Weight(10000000)
                        )
                );

        repository.insertSpecifications(id, specifications);

        var stored = jdbcTemplate.queryForMap(
                """
                SELECT
                    length_mm,
                    width_mm,
                    height_mm,
                    weight_specifications_present,
                    empty_weight_grams,
                    gross_weight_rating_grams,
                    rated_payload_capacity_grams
                FROM equipment_specifications
                WHERE equipment_id = ?
                """,
                id
        );

        assertEquals(12000L, stored.get("length_mm"));
        assertEquals(2500L, stored.get("width_mm"));
        assertEquals(4000L, stored.get("height_mm"));
        assertEquals(true, stored.get("weight_specifications_present"));
        assertEquals(5000000L, stored.get("empty_weight_grams"));
        assertEquals(15000000L, stored.get("gross_weight_rating_grams"));
        assertEquals(10000000L, stored.get("rated_payload_capacity_grams"));
    }


    @Test
    void retrievesEquipmentSpecificationsById() {
        UUID id = UUID.randomUUID();

        Equipment equipment = new Equipment(
                id,
                "CHARRON Test Container",
                EquipmentCategory.CONTAINER,
                null
        );

        repository.insertIdentity(equipment);

        EquipmentSpecifications original =
                new EquipmentSpecifications(
                        new Dimensions(6000, 2400, 2600),
                        new WeightSpecifications(
                                new Weight(2200000),
                                new Weight(30000000),
                                null
                        )
                );

        repository.insertSpecifications(id, original);

        EquipmentSpecifications retrieved =
                repository.findSpecificationsByEquipmentId(id)
                        .orElseThrow();

        assertEquals(original, retrieved);
    }


    @Test
    void retrievesSpecificationsWithoutDimensions() {
        UUID id = UUID.randomUUID();

        repository.insertIdentity(new Equipment(
                id,
                "CHARRON Forklift",
                EquipmentCategory.MATERIAL_HANDLING,
                null
        ));

        EquipmentSpecifications original =
                new EquipmentSpecifications(
                        null,
                        new WeightSpecifications(
                                new Weight(4500000),
                                new Weight(6500000),
                                null
                        )
                );

        repository.insertSpecifications(id, original);

        EquipmentSpecifications retrieved =
                repository.findSpecificationsByEquipmentId(id)
                        .orElseThrow();

        assertEquals(original, retrieved);
        assertNull(retrieved.exteriorDimensions());
    }

    @Test
    void retrievesSpecificationsWithoutWeights() {
        UUID id = UUID.randomUUID();

        repository.insertIdentity(new Equipment(
                id,
                "CHARRON Container",
                EquipmentCategory.CONTAINER,
                null
        ));

        EquipmentSpecifications original =
                new EquipmentSpecifications(
                        new Dimensions(6000, 2400, 2600),
                        null
                );

        repository.insertSpecifications(id, original);

        EquipmentSpecifications retrieved =
                repository.findSpecificationsByEquipmentId(id)
                        .orElseThrow();

        assertEquals(original, retrieved);
        assertNull(retrieved.weightSpecifications());
    }

    @Test
    void preservesPresentButUnknownWeightSpecifications() {
        UUID id = UUID.randomUUID();

        repository.insertIdentity(new Equipment(
                id,
                "CHARRON Specialized Equipment",
                EquipmentCategory.SPECIALIZED,
                null
        ));

        EquipmentSpecifications original =
                new EquipmentSpecifications(
                        null,
                        new WeightSpecifications(null, null, null)
                );

        repository.insertSpecifications(id, original);

        EquipmentSpecifications retrieved =
                repository.findSpecificationsByEquipmentId(id)
                        .orElseThrow();

        assertEquals(original, retrieved);
        assertNull(retrieved.exteriorDimensions());
        assertNotNull(retrieved.weightSpecifications());
        assertNull(retrieved.weightSpecifications().emptyWeight());
        assertNull(retrieved.weightSpecifications().grossWeightRating());
        assertNull(retrieved.weightSpecifications().ratedPayloadCapacity());
    }


}
