package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.MovementManifestRepository;
import com.charron.api.movement.domain.MovementManifest;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcMovementManifestRepository
        implements MovementManifestRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public JdbcMovementManifestRepository(
            JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper
    ) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "JdbcTemplate is required"
        );

        this.jsonMapper = Objects.requireNonNull(
                jsonMapper,
                "JsonMapper is required"
        );
    }

    private final RowMapper<MovementManifest> rowMapper =
            (rs, rowNum) -> deserialize(
                    rs.getString("snapshot")
            );

    @Override
    public void save(MovementManifest manifest) {

        Objects.requireNonNull(
                manifest,
                "Movement manifest is required"
        );

        jdbcTemplate.update("""
                INSERT INTO movement_manifests (
                    id,
                    movement_id,
                    version,
                    snapshot,
                    generated_at
                )
                VALUES (?, ?, ?, CAST(? AS jsonb), ?)
                """,
                manifest.id(),
                manifest.movementId(),
                manifest.version(),
                serialize(manifest),
                Timestamp.from(manifest.generatedAt())
        );
    }

    @Override
    public Optional<MovementManifest> findById(
            UUID manifestId
    ) {

        Objects.requireNonNull(manifestId);

        List<MovementManifest> results =
                jdbcTemplate.query("""
                        SELECT snapshot
                        FROM movement_manifests
                        WHERE id = ?
                        """,
                        rowMapper,
                        manifestId
                );

        return results.stream().findFirst();
    }


    @Override
    public Optional<MovementManifest> findByMovementIdAndVersion(
            UUID movementId,
            int version
    ) {

        Objects.requireNonNull(movementId);

        if (version < 1) {
            throw new IllegalArgumentException(
                    "Manifest version must be positive"
            );
        }

        List<MovementManifest> results =
                jdbcTemplate.query("""
                        SELECT snapshot
                        FROM movement_manifests
                        WHERE movement_id = ?
                          AND version = ?
                        """,
                        rowMapper,
                        movementId,
                        version
                );

        return results.stream().findFirst();
    }

    @Override
    public Optional<MovementManifest> findLatestByMovementId(
            UUID movementId
    ) {

        Objects.requireNonNull(movementId);

        List<MovementManifest> results =
                jdbcTemplate.query("""
                        SELECT snapshot
                        FROM movement_manifests
                        WHERE movement_id = ?
                        ORDER BY version DESC
                        LIMIT 1
                        """,
                        rowMapper,
                        movementId
                );

        return results.stream().findFirst();
    }

    @Override
    public List<MovementManifest> findByMovementId(
            UUID movementId
    ) {

        Objects.requireNonNull(movementId);

        return jdbcTemplate.query("""
                SELECT snapshot
                FROM movement_manifests
                WHERE movement_id = ?
                ORDER BY version DESC
                """,
                rowMapper,
                movementId
        );
    }

    @Override
    public int findLatestVersion(UUID movementId) {

        Objects.requireNonNull(movementId);

        Integer version = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(version), 0)
                FROM movement_manifests
                WHERE movement_id = ?
                """,
                Integer.class,
                movementId
        );

        return version == null ? 0 : version;
    }

    private String serialize(MovementManifest manifest) {

        try {
            return jsonMapper.writeValueAsString(manifest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to serialize movement manifest: "
                            + manifest.id(),
                    exception
            );
        }
    }

    private MovementManifest deserialize(String snapshot) {

        try {
            return jsonMapper.readValue(
                    snapshot,
                    MovementManifest.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to deserialize movement manifest",
                    exception
            );
        }
    }
}