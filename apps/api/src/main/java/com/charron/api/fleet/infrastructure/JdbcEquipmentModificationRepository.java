
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentModificationRepository;
import com.charron.api.fleet.domain.EquipmentModification;
import com.charron.api.fleet.domain.EquipmentModificationType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public Optional<EquipmentModification> findById(
            UUID modificationId
    ) {
        Objects.requireNonNull(
                modificationId, "Modification ID is required"
        );

        String sql = """
            SELECT
                id,
                equipment_id,
                configuration_id,
                type,
                description,
                effective_at,
                recorded_at,
                removed_at
            FROM equipment_modifications
            WHERE id = ?
            """;

        List<EquipmentModification> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    java.sql.Timestamp removedAt =
                            rs.getTimestamp("removed_at");

                    return new EquipmentModification(
                            rs.getObject("id", UUID.class),
                            rs.getObject("equipment_id", UUID.class),
                            rs.getObject("configuration_id", UUID.class),
                            EquipmentModificationType.valueOf(
                                    rs.getString("type")
                            ),
                            rs.getString("description"),
                            rs.getTimestamp("effective_at").toInstant(),
                            rs.getTimestamp("recorded_at").toInstant(),
                            removedAt == null
                                    ? null
                                    : removedAt.toInstant()
                    );
                },
                modificationId
        );

        return results.stream().findFirst();
    }

}
