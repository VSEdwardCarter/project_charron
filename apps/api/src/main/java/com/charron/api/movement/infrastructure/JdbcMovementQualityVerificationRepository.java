package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.MovementQualityVerificationRepository;
import com.charron.api.movement.domain.MovementQualityVerification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
public class JdbcMovementQualityVerificationRepository
        implements MovementQualityVerificationRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<MovementQualityVerification> ROW_MAPPER =
            (rs, rowNum) -> new MovementQualityVerification(
                    rs.getObject("id", UUID.class),
                    rs.getObject("manifest_id", UUID.class),
                    rs.getObject("reviewer_id", UUID.class),
                    rs.getBoolean("equipment_assignment_verified"),
                    rs.getBoolean("equipment_inspection_completed"),
                    rs.getBoolean("cargo_reconciliation_verified"),
                    rs.getBoolean("loading_arrangement_verified"),
                    rs.getBoolean("cargo_securement_verified"),
                    rs.getBoolean("documentation_verified"),
                    rs.getTimestamp("verified_at").toInstant(),
                    rs.getString("remarks")
            );

    public JdbcMovementQualityVerificationRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void save(MovementQualityVerification verification) {

        Objects.requireNonNull(verification);

        jdbcTemplate.update(
                """
                INSERT INTO movement_quality_verifications (
                    id,
                    manifest_id,
                    reviewer_id,
                    equipment_assignment_verified,
                    equipment_inspection_completed,
                    cargo_reconciliation_verified,
                    loading_arrangement_verified,
                    cargo_securement_verified,
                    documentation_verified,
                    verified_at,
                    remarks
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                verification.id(),
                verification.manifestId(),
                verification.reviewerId(),
                verification.equipmentAssignmentVerified(),
                verification.equipmentInspectionCompleted(),
                verification.cargoReconciliationVerified(),
                verification.loadingArrangementVerified(),
                verification.cargoSecurementVerified(),
                verification.documentationVerified(),
                Timestamp.from(verification.verifiedAt()),
                verification.remarks()
        );
    }

    @Override
    public List<MovementQualityVerification> findByManifestId(
            UUID manifestId
    ) {

        return jdbcTemplate.query(
                """
                SELECT *
                FROM movement_quality_verifications
                WHERE manifest_id = ?
                ORDER BY verified_at DESC, id DESC
                """,
                ROW_MAPPER,
                manifestId
        );
    }

    @Override
    public List<MovementQualityVerification> findByReviewerId(
            UUID reviewerId
    ) {

        return jdbcTemplate.query(
                """
                SELECT *
                FROM movement_quality_verifications
                WHERE reviewer_id = ?
                ORDER BY verified_at DESC, id DESC
                """,
                ROW_MAPPER,
                reviewerId
        );
    }

    @Override
    public boolean existsByManifestId(UUID manifestId) {

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM movement_quality_verifications
                            WHERE manifest_id = ?
                        )
                        """,
                        Boolean.class,
                        manifestId
                )
        );
    }

    @Override
    public boolean existsForCurrentOperationalRevision(
            UUID manifestId
    ) {

        Objects.requireNonNull(manifestId);

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM movement_quality_verifications qv

                            JOIN movement_manifests mm
                                ON mm.id = qv.manifest_id

                            JOIN movements m
                                ON m.id = mm.movement_id

                            WHERE qv.manifest_id = ?
                              AND qv.operational_revision =
                                  m.operational_revision
                        )
                        """,
                        Boolean.class,
                        manifestId
                )
        );
    }
}