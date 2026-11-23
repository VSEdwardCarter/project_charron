
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.LoadingPlanRepository;
import com.charron.api.loading.domain.LoadingPlan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLoadingPlanRepository implements LoadingPlanRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<LoadingPlan> rowMapper =
            (rs, rowNum) -> new LoadingPlan(
                    rs.getObject("id", UUID.class),
                    rs.getObject("movement_id", UUID.class),
                    rs.getObject("equipment_id", UUID.class),
                    rs.getObject("cargo_space_id", UUID.class),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );

    public JdbcLoadingPlanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void insert(LoadingPlan plan) {
        Objects.requireNonNull(plan, "Loading plan is required");

        jdbcTemplate.update(
                """
                INSERT INTO loading_plans (
                    id,
                    movement_id,
                    equipment_id,
                    cargo_space_id,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                plan.id(),
                plan.movementId(),
                plan.equipmentId(),
                plan.cargoSpaceId(),
                Timestamp.from(plan.createdAt()),
                Timestamp.from(plan.updatedAt())
        );
    }

    @Override
    public Optional<LoadingPlan> findById(UUID id) {
        List<LoadingPlan> results = jdbcTemplate.query(
                """
                SELECT
                    id,
                    movement_id,
                    equipment_id,
                    cargo_space_id,
                    created_at,
                    updated_at
                FROM loading_plans
                WHERE id = ?
                """,
                rowMapper,
                id
        );

        return results.stream().findFirst();
    }

    @Override
    public List<LoadingPlan> findByMovementId(UUID movementId) {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    movement_id,
                    equipment_id,
                    cargo_space_id,
                    created_at,
                    updated_at
                FROM loading_plans
                WHERE movement_id = ?
                ORDER BY created_at, id
                """,
                rowMapper,
                movementId
        );
    }

    @Override
    public boolean existsByMovementAndCargoSpace(
            UUID movementId,
            UUID cargoSpaceId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM loading_plans
                WHERE movement_id = ?
                  AND cargo_space_id = ?
                """,
                Integer.class,
                movementId,
                cargoSpaceId
        );

        return count != null && count > 0;
    }
}
