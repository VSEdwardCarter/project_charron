
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.LoadingPlanCargoRepository;
import com.charron.api.loading.domain.LoadingPlanCargo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLoadingPlanCargoRepository
        implements LoadingPlanCargoRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<LoadingPlanCargo> rowMapper =
            (rs, rowNum) -> new LoadingPlanCargo(
                    rs.getObject("id", UUID.class),
                    rs.getObject("loading_plan_id", UUID.class),
                    rs.getObject("cargo_id", UUID.class),
                    rs.getInt("quantity"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );

    public JdbcLoadingPlanCargoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void insert(LoadingPlanCargo cargoLine) {
        Objects.requireNonNull(cargoLine);

        jdbcTemplate.update(
                """
                INSERT INTO loading_plan_cargo (
                    id,
                    loading_plan_id,
                    cargo_id,
                    quantity,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                cargoLine.id(),
                cargoLine.loadingPlanId(),
                cargoLine.cargoId(),
                cargoLine.quantity(),
                Timestamp.from(cargoLine.createdAt()),
                Timestamp.from(cargoLine.updatedAt())
        );
    }

    @Override
    public Optional<LoadingPlanCargo> findById(UUID id) {
        return jdbcTemplate.query(
                """
                SELECT id, loading_plan_id, cargo_id,
                       quantity, created_at, updated_at
                FROM loading_plan_cargo
                WHERE id = ?
                """,
                rowMapper,
                id
        ).stream().findFirst();
    }

    @Override
    public List<LoadingPlanCargo> findByLoadingPlanId(
            UUID loadingPlanId
    ) {
        return jdbcTemplate.query(
                """
                SELECT id, loading_plan_id, cargo_id,
                       quantity, created_at, updated_at
                FROM loading_plan_cargo
                WHERE loading_plan_id = ?
                ORDER BY created_at, id
                """,
                rowMapper,
                loadingPlanId
        );
    }

    @Override
    public boolean existsByLoadingPlanAndCargo(
            UUID loadingPlanId,
            UUID cargoId
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM loading_plan_cargo
                WHERE loading_plan_id = ?
                  AND cargo_id = ?
                """,
                Integer.class,
                loadingPlanId,
                cargoId
        );

        return count != null && count > 0;
    }

    @Override
    public void updateQuantity(LoadingPlanCargo cargoLine) {
        Objects.requireNonNull(cargoLine);

        int updated = jdbcTemplate.update(
                """
                UPDATE loading_plan_cargo
                SET quantity = ?,
                    updated_at = ?
                WHERE id = ?
                  AND loading_plan_id = ?
                """,
                cargoLine.quantity(),
                Timestamp.from(cargoLine.updatedAt()),
                cargoLine.id(),
                cargoLine.loadingPlanId()
        );

        if (updated != 1) {
            throw new IllegalStateException(
                    "Cargo line does not exist: " + cargoLine.id()
            );
        }
    }

    @Override
    public void deleteById(UUID id) {
        jdbcTemplate.update(
                """
                DELETE FROM loading_plan_cargo
                WHERE id = ?
                """,
                id
        );
    }
}
