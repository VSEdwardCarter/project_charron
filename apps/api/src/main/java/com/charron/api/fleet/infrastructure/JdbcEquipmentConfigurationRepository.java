
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentConfigurationRepository;
import com.charron.api.fleet.domain.ConfigurationWeightAdjustment;
import com.charron.api.fleet.domain.EquipmentCapability;
import com.charron.api.fleet.domain.EquipmentConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class JdbcEquipmentConfigurationRepository
        implements EquipmentConfigurationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEquipmentConfigurationRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void insert(EquipmentConfiguration configuration) {
        Objects.requireNonNull(
                configuration, "Equipment configuration is required"
        );

        ConfigurationWeightAdjustment adjustment =
                configuration.getWeightAdjustment();

        String sql = """
                INSERT INTO equipment_configurations (
                    id,
                    equipment_id,
                    name,
                    active,
                    weight_adjustment_grams
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        int rows = jdbcTemplate.update(
                sql,
                configuration.getId(),
                configuration.getEquipmentId(),
                configuration.getName(),
                configuration.isActive(),
                adjustment == null ? null : adjustment.grams()
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "Expected exactly one equipment configuration row"
            );
        }
    }

    @Override
    public Optional<EquipmentConfiguration> findById(
            UUID configurationId
    ) {
        Objects.requireNonNull(
                configurationId, "Configuration ID is required"
        );

        String sql = """
            SELECT
                id,
                equipment_id,
                name,
                active,
                weight_adjustment_grams
            FROM equipment_configurations
            WHERE id = ?
            """;

        List<EquipmentConfiguration> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    EquipmentConfiguration configuration =
                            new EquipmentConfiguration(
                                    rs.getObject("id", UUID.class),
                                    rs.getObject("equipment_id", UUID.class),
                                    rs.getString("name"),
                                    rs.getBoolean("active")
                            );

                    Long adjustmentGrams = rs.getObject(
                            "weight_adjustment_grams",
                            Long.class
                    );

                    if (adjustmentGrams != null) {
                        configuration.updateWeightAdjustment(
                                new ConfigurationWeightAdjustment(
                                        adjustmentGrams
                                )
                        );
                    }

                    return configuration;
                },
                configurationId
        );

        return results.stream().findFirst();
    }


    @Override
    public void insertCapability(
            UUID configurationId,
            EquipmentCapability capability
    ) {
        Objects.requireNonNull(
                configurationId, "Configuration ID is required"
        );
        Objects.requireNonNull(
                capability, "Configuration capability is required"
        );

        String sql = """
            INSERT INTO equipment_configuration_capabilities (
                configuration_id,
                capability
            )
            VALUES (?, ?)
            """;

        int rows = jdbcTemplate.update(
                sql,
                configurationId,
                capability.name()
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "Expected exactly one configuration capability row"
            );
        }
    }


    @Override
    public Set<EquipmentCapability> findCapabilitiesByConfigurationId(
            UUID configurationId
    ) {
        Objects.requireNonNull(
                configurationId, "Configuration ID is required"
        );

        String sql = """
            SELECT capability
            FROM equipment_configuration_capabilities
            WHERE configuration_id = ?
            """;

        List<EquipmentCapability> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> EquipmentCapability.valueOf(
                        rs.getString("capability")
                ),
                configurationId
        );

        Set<EquipmentCapability> capabilities =
                EnumSet.noneOf(EquipmentCapability.class);

        capabilities.addAll(results);

        return capabilities;
    }

    @Override
    public boolean removeCapability(
            UUID configurationId,
            EquipmentCapability capability
    ) {
        Objects.requireNonNull(
                configurationId, "Configuration ID is required"
        );
        Objects.requireNonNull(
                capability, "Configuration capability is required"
        );

        String sql = """
            DELETE FROM equipment_configuration_capabilities
            WHERE configuration_id = ?
              AND capability = ?
            """;

        int rows = jdbcTemplate.update(
                sql,
                configurationId,
                capability.name()
        );

        return rows == 1;
    }

    @Override
    public boolean updateDetails(EquipmentConfiguration configuration) {
        Objects.requireNonNull(
                configuration, "Equipment configuration is required"
        );

        String sql = """
            UPDATE equipment_configurations
            SET name = ?,
                active = ?
            WHERE id = ?
              AND equipment_id = ?
            """;

        int rows = jdbcTemplate.update(
                sql,
                configuration.getName(),
                configuration.isActive(),
                configuration.getId(),
                configuration.getEquipmentId()
        );

        return rows == 1;
    }

    @Override
    public boolean updateWeightAdjustment(
            EquipmentConfiguration configuration
    ) {
        Objects.requireNonNull(
                configuration, "Equipment configuration is required"
        );

        ConfigurationWeightAdjustment adjustment =
                configuration.getWeightAdjustment();

        String sql = """
            UPDATE equipment_configurations
            SET weight_adjustment_grams = ?
            WHERE id = ?
              AND equipment_id = ?
            """;

        int rows = jdbcTemplate.update(
                sql,
                adjustment == null ? null : adjustment.grams(),
                configuration.getId(),
                configuration.getEquipmentId()
        );

        return rows == 1;
    }


    @Override
    public List<EquipmentConfiguration> findByEquipmentId(
            UUID equipmentId
    ) {
        Objects.requireNonNull(
                equipmentId,
                "Equipment ID is required"
        );

        String sql = """
            SELECT id, equipment_id, name, active
            FROM equipment_configurations
            WHERE equipment_id = ?
            ORDER BY name ASC, id ASC
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new EquipmentConfiguration(
                        rs.getObject("id", UUID.class),
                        rs.getObject("equipment_id", UUID.class),
                        rs.getString("name"),
                        rs.getBoolean("active")
                ),
                equipmentId
        );
    }


}
