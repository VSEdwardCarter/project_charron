
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.CargoSpaceOwnershipChecker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcCargoSpaceOwnershipChecker
        implements CargoSpaceOwnershipChecker {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCargoSpaceOwnershipChecker(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate =
                Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public boolean belongsToEquipment(
            UUID cargoSpaceId,
            UUID equipmentId
    ) {
        Objects.requireNonNull(cargoSpaceId);
        Objects.requireNonNull(equipmentId);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM equipment_cargo_spaces
                WHERE id = ?
                  AND equipment_id = ?
                """,
                Integer.class,
                cargoSpaceId,
                equipmentId
        );

        return count != null && count > 0;
    }
}
