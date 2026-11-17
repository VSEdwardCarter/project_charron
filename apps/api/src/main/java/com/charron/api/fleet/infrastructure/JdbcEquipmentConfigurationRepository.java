
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentConfigurationRepository;
import com.charron.api.fleet.domain.ConfigurationWeightAdjustment;
import com.charron.api.fleet.domain.EquipmentConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;

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
}
