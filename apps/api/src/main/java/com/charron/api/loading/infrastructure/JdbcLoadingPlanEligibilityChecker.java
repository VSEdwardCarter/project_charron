
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.LoadingPlanEligibilityChecker;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcLoadingPlanEligibilityChecker
        implements LoadingPlanEligibilityChecker {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoadingPlanEligibilityChecker(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public boolean movementExists(UUID movementId) {
        Objects.requireNonNull(movementId);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM movements
                WHERE id = ?
                """,
                Integer.class,
                movementId
        );

        return count != null && count > 0;
    }

    @Override
    public boolean equipmentAssignedToMovement(
            UUID movementId,
            UUID equipmentId
    ) {
        Objects.requireNonNull(movementId);
        Objects.requireNonNull(equipmentId);

        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM movement_equipment_assignments
                WHERE movement_id = ?
                  AND equipment_id = ?
                """,
                Integer.class,
                movementId,
                equipmentId
        );

        return count != null && count > 0;
    }
}
