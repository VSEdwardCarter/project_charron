
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentModificationRepository;
import com.charron.api.fleet.domain.EquipmentModification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class JdbcEquipmentModificationRepository
        implements EquipmentModificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEquipmentModificationRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void insert(EquipmentModification modification) {
        Objects.requireNonNull(
                modification, "Equipment modification is required"
        );

        String sql = """
                INSERT INTO equipment_modifications (
                    id,
                    equipment_id,
                    configuration_id,
                    type,
                    description,
                    effective_at,
                    recorded_at,
                    removed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        int rows = jdbcTemplate.update(
                sql,
                modification.id(),
                modification.equipmentId(),
                modification.configurationId(),
                modification.type().name(),
                modification.description(),
                java.sql.Timestamp.from(modification.effectiveAt()),
                java.sql.Timestamp.from(modification.recordedAt()),
                modification.removedAt() == null
                        ? null
                        : java.sql.Timestamp.from(modification.removedAt())
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "Expected exactly one equipment modification row"
            );
        }
    }
}
