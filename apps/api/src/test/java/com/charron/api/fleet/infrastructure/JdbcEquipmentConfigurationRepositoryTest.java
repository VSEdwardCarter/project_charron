
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentConfigurationRepository;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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


    @Test
    void insertsConfigurationCapabilitiesIndependentlyOfEquipment() {
        UUID equipmentId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Refrigerated Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        equipmentRepository.insertCapability(
                equipmentId,
                EquipmentCapability.CARGO_TRANSPORT
        );

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        configurationId,
                        equipmentId,
                        "Refrigerated Configuration",
                        true
                );

        configurationRepository.insert(configuration);

        configurationRepository.insertCapability(
                configurationId,
                EquipmentCapability.TEMPERATURE_CONTROL
        );

        configurationRepository.insertCapability(
                configurationId,
                EquipmentCapability.ENCLOSED_CARGO
        );

        Integer configurationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM equipment_configuration_capabilities
                WHERE configuration_id = ?
                """,
                Integer.class,
                configurationId
        );

        assertEquals(2, configurationCount);

        Integer equipmentCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM equipment_capabilities
                WHERE equipment_id = ?
                """,
                Integer.class,
                equipmentId
        );

        assertEquals(1, equipmentCount);
    }

    @Test
    void retrievesConfigurationCapabilitiesById() {
        UUID equipmentId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        configurationId,
                        equipmentId,
                        "Refrigerated Configuration",
                        true
                );

        configurationRepository.insert(configuration);

        configurationRepository.insertCapability(
                configurationId,
                EquipmentCapability.TEMPERATURE_CONTROL
        );

        configurationRepository.insertCapability(
                configurationId,
                EquipmentCapability.ENCLOSED_CARGO
        );

        Set<EquipmentCapability> retrieved =
                configurationRepository.findCapabilitiesByConfigurationId(
                        configurationId
                );

        assertEquals(
                Set.of(
                        EquipmentCapability.TEMPERATURE_CONTROL,
                        EquipmentCapability.ENCLOSED_CARGO
                ),
                retrieved
        );

        assertTrue(
                equipmentRepository.findCapabilitiesByEquipmentId(
                        equipmentId
                ).isEmpty()
        );
    }

    @Test
    void removesOnlySpecifiedConfigurationCapability() {
        UUID equipmentId = UUID.randomUUID();
        UUID configurationId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Refrigerated Trailer",
                EquipmentCategory.TRAILER,
                null
        ));

        equipmentRepository.insertCapability(
                equipmentId,
                EquipmentCapability.CARGO_TRANSPORT
        );

        EquipmentConfiguration configuration =
                new EquipmentConfiguration(
                        configurationId,
                        equipmentId,
                        "Refrigerated Configuration",
                        true
                );

        configurationRepository.insert(configuration);

        configurationRepository.insertCapability(
                configurationId,
                EquipmentCapability.TEMPERATURE_CONTROL
        );

        configurationRepository.insertCapability(
                configurationId,
                EquipmentCapability.ENCLOSED_CARGO
        );

        boolean removed = configurationRepository.removeCapability(
                configurationId,
                EquipmentCapability.TEMPERATURE_CONTROL
        );

        assertTrue(removed);

        assertEquals(
                Set.of(EquipmentCapability.ENCLOSED_CARGO),
                configurationRepository.findCapabilitiesByConfigurationId(
                        configurationId
                )
        );

        assertEquals(
                Set.of(EquipmentCapability.CARGO_TRANSPORT),
                equipmentRepository.findCapabilitiesByEquipmentId(
                        equipmentId
                )
        );

        assertFalse(
                configurationRepository.removeCapability(
                        configurationId,
                        EquipmentCapability.TEMPERATURE_CONTROL
                )
        );
    }


    @Test
    void updatesConfigurationNameAndActiveStatus() {
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
                        "Original Configuration",
                        false
                );

        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(500000)
        );

        configurationRepository.insert(configuration);

        configuration.setName("Updated Heavy Haul Configuration");
        configuration.activate();

        boolean updated = configurationRepository.updateDetails(
                configuration
        );

        assertTrue(updated);

        EquipmentConfiguration retrieved =
                configurationRepository.findById(configurationId)
                        .orElseThrow();

        assertEquals(
                "Updated Heavy Haul Configuration",
                retrieved.getName()
        );

        assertTrue(retrieved.isActive());

        assertEquals(
                new ConfigurationWeightAdjustment(500000),
                retrieved.getWeightAdjustment()
        );
    }

    @Test
    void updatesAndRemovesConfigurationWeightAdjustment() {
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

        configurationRepository.insert(configuration);

        // Add a positive weight adjustment.
        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(750000)
        );

        assertTrue(
                configurationRepository.updateWeightAdjustment(
                        configuration
                )
        );

        assertEquals(
                new ConfigurationWeightAdjustment(750000),
                configurationRepository.findById(configurationId)
                        .orElseThrow()
                        .getWeightAdjustment()
        );

        // Change to a negative weight adjustment.
        configuration.updateWeightAdjustment(
                new ConfigurationWeightAdjustment(-250000)
        );

        assertTrue(
                configurationRepository.updateWeightAdjustment(
                        configuration
                )
        );

        assertEquals(
                new ConfigurationWeightAdjustment(-250000),
                configurationRepository.findById(configurationId)
                        .orElseThrow()
                        .getWeightAdjustment()
        );

        // Remove the adjustment entirely.
        configuration.removeWeightAdjustment();

        assertTrue(
                configurationRepository.updateWeightAdjustment(
                        configuration
                )
        );

        EquipmentConfiguration retrieved =
                configurationRepository.findById(configurationId)
                        .orElseThrow();

        assertNull(retrieved.getWeightAdjustment());

        // Unrelated configuration fields remain unchanged.
        assertEquals(
                "Heavy Haul Configuration",
                retrieved.getName()
        );

        assertTrue(retrieved.isActive());
    }

}
