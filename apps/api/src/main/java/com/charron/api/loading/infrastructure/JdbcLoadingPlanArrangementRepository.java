
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.LoadingPlanArrangementRepository;
import com.charron.api.loading.domain.LoadingPlanArrangement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLoadingPlanArrangementRepository
        implements LoadingPlanArrangementRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<LoadingPlanArrangement> rowMapper =
            (rs, rowNum) -> new LoadingPlanArrangement(
                    rs.getObject("loading_plan_id", UUID.class),
                    rs.getInt("format_version"),
                    rs.getString("request_snapshot"),
                    rs.getString("preview_snapshot"),
                    rs.getString("status"),
                    rs.getTimestamp("generated_at").toInstant(),
                    rs.getTimestamp("saved_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );

    public JdbcLoadingPlanArrangementRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void save(LoadingPlanArrangement arrangement) {
        Objects.requireNonNull(
                arrangement,
                "Loading plan arrangement is required"
        );

        jdbcTemplate.update(
                """
                INSERT INTO loading_plan_arrangements (
                    loading_plan_id,
                    format_version,
                    request_snapshot,
                    preview_snapshot,
                    status,
                    generated_at,
                    saved_at,
                    updated_at
                )
                VALUES (
                    ?, ?, CAST(? AS jsonb), CAST(? AS jsonb),
                    ?, ?, ?, ?
                )
                ON CONFLICT (loading_plan_id)
                DO UPDATE SET
                    format_version = EXCLUDED.format_version,
                    request_snapshot = EXCLUDED.request_snapshot,
                    preview_snapshot = EXCLUDED.preview_snapshot,
                    status = EXCLUDED.status,
                    generated_at = EXCLUDED.generated_at,
                    updated_at = EXCLUDED.updated_at
                """,
                arrangement.loadingPlanId(),
                arrangement.formatVersion(),
                arrangement.requestSnapshot(),
                arrangement.previewSnapshot(),
                arrangement.status(),
                Timestamp.from(arrangement.generatedAt()),
                Timestamp.from(arrangement.savedAt()),
                Timestamp.from(arrangement.updatedAt())
        );
    }

    @Override
    public Optional<LoadingPlanArrangement> findByLoadingPlanId(
            UUID loadingPlanId
    ) {
        List<LoadingPlanArrangement> results = jdbcTemplate.query(
                """
                SELECT
                    loading_plan_id,
                    format_version,
                    request_snapshot,
                    preview_snapshot,
                    status,
                    generated_at,
                    saved_at,
                    updated_at
                FROM loading_plan_arrangements
                WHERE loading_plan_id = ?
                """,
                rowMapper,
                loadingPlanId
        );

        return results.stream().findFirst();
    }

    @Override
    public void deleteByLoadingPlanId(UUID loadingPlanId) {
        jdbcTemplate.update(
                """
                DELETE FROM loading_plan_arrangements
                WHERE loading_plan_id = ?
                """,
                loadingPlanId
        );
    }
}
