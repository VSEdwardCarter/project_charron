
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentConfigurationRepository;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.ConfigurationWeightAdjustment;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.domain.EquipmentConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class JdbcEquipmentConfigurationRepositoryTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private EquipmentConfigurationRepository configurationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsEquipmentConfiguration() {
        UUID equipmentId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Tractor",
                EquipmentCategory.TRACTOR,
                null
        ));

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        configurationId,
                        equipmentId,
                        "Heavy Haul Configuration",
                        true
                );

        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(750000)
        );

        configurationRepository.insert(configuration);

        var stored = jdbcTemplate.queryForMap(
                """
                SELECT
                    equipment_id,
                    name,
                    active,
                    weight_adjustment_grams
                FROM equipment_configurations
                WHERE id = ?
                """,
                configurationId
        );

        assertEquals(equipmentId, stored.get("equipment_id"));
        assertEquals(
                "Heavy Haul Configuration",
                stored.get("name")
        );
        assertEquals(true, stored.get("active"));
        assertEquals(
                750000L,
                stored.get("weight_adjustment_grams")
        );
    }


    @Test
    void retrievesEquipmentConfigurationById() {
        UUID equipmentId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        EquipmentConfiguration original =
                new EquipmentConfiguration(
                        configurationId,
                        equipmentId,
                        "Lightweight Configuration",
                        false
                );

        original.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(-250000)
        );

        configurationRepository.insert(original);

        EquipmentConfiguration retrieved =
                configurationRepository.findById(configurationId)
                        .orElseThrow();

        assertEquals(original.getId(), retrieved.getId());
        assertEquals(
                original.getEquipmentId(),
                retrieved.getEquipmentId()
        );
        assertEquals(original.getName(), retrieved.getName());
        assertEquals(original.isActive(), retrieved.isActive());

        assertEquals(
                original.getWeightAdjustment(),
                retrieved.getWeightAdjustment()
        );
    }

    @Test
    void returnsEmptyWhenConfigurationDoesNotExist() {
        UUID nonexistentId = UUID.randomUUID();

        assertTrue(
                configurationRepository.findById(nonexistentId)
                        .isEmpty()
        );
    }
}
