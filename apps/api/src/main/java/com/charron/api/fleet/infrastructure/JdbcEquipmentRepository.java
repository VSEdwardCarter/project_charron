
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.domain.Equipment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
public class JdbcEquipmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEquipmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    public void insertIdentity(Equipment equipment) {
        Objects.requireNonNull(
                equipment,
                "Equipment is required"
        );

        String sql = """
                INSERT INTO equipment (
                    id,
                    name,
                    category,
                    vin
                )
                VALUES (?, ?, ?, ?)
                """;

        int rows = jdbcTemplate.update(
                sql,
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory().name(),
                equipment.getVin()
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "Expected exactly one equipment row to be inserted"
            );
        }
    }
}
