
package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.EquipmentExistenceChecker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcEquipmentExistenceChecker
        implements EquipmentExistenceChecker {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEquipmentExistenceChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean existsById(UUID equipmentId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM equipment WHERE id = ?",
                Integer.class,
                equipmentId
        );

        return count != null && count > 0;
    }
}
