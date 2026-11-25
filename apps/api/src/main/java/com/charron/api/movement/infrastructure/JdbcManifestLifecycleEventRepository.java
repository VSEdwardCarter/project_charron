package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.ManifestLifecycleEventRepository;
import com.charron.api.movement.domain.ManifestLifecycleEvent;
import com.charron.api.movement.domain.ManifestStatus;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcManifestLifecycleEventRepository
        implements ManifestLifecycleEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcManifestLifecycleEventRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "JdbcTemplate is required"
        );
    }

    private static final RowMapper<ManifestLifecycleEvent>
            EVENT_ROW_MAPPER = (rs, rowNum) ->
            new ManifestLifecycleEvent(
                    rs.getObject("id", UUID.class),
                    rs.getObject("manifest_id", UUID.class),
                    ManifestStatus.valueOf(
                            rs.getString("previous_status")
                    ),
                    ManifestStatus.valueOf(
                            rs.getString("new_status")
                    ),
                    rs.getObject("actor_id", UUID.class),
                    rs.getTimestamp("recorded_at").toInstant(),
                    rs.getString("remarks")
            );

    @Override
    public void save(ManifestLifecycleEvent event) {

        Objects.requireNonNull(
                event,
                "Manifest lifecycle event is required"
        );

        jdbcTemplate.update("""
                INSERT INTO manifest_lifecycle_events (
                    id,
                    manifest_id,
                    previous_status,
                    new_status,
                    actor_id,
                    recorded_at,
                    remarks
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                event.id(),
                event.manifestId(),
                event.previousStatus().name(),
                event.newStatus().name(),
                event.actorId(),
                Timestamp.from(event.recordedAt()),
                event.remarks()
        );
    }

    @Override
    public Optional<ManifestLifecycleEvent> findById(
            UUID eventId
    ) {

        Objects.requireNonNull(
                eventId,
                "Event ID is required"
        );

        List<ManifestLifecycleEvent> results =
                jdbcTemplate.query("""
                        SELECT
                            id,
                            manifest_id,
                            previous_status,
                            new_status,
                            actor_id,
                            recorded_at,
                            remarks
                        FROM manifest_lifecycle_events
                        WHERE id = ?
                        """,
                        EVENT_ROW_MAPPER,
                        eventId
                );

        return results.stream().findFirst();
    }

    @Override
    public List<ManifestLifecycleEvent> findByManifestId(
            UUID manifestId
    ) {

        Objects.requireNonNull(
                manifestId,
                "Manifest ID is required"
        );

        return jdbcTemplate.query("""
                SELECT
                    id,
                    manifest_id,
                    previous_status,
                    new_status,
                    actor_id,
                    recorded_at,
                    remarks
                FROM manifest_lifecycle_events
                WHERE manifest_id = ?
                ORDER BY
                    recorded_at ASC,
                    CASE previous_status
                        WHEN 'DRAFT' THEN 1
                        WHEN 'REVIEWED' THEN 2
                        WHEN 'ISSUED' THEN 3
                    END ASC,
                    id ASC
                """,
                EVENT_ROW_MAPPER,
                manifestId
        );
    }
}