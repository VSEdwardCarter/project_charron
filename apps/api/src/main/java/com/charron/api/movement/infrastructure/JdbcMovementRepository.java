
package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.MovementRepository;
import com.charron.api.movement.domain.Movement;
import com.charron.api.movement.domain.MovementStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcMovementRepository implements MovementRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMovementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "JdbcTemplate is required"
        );
    }

    private static final RowMapper<Movement> MOVEMENT_ROW_MAPPER =
            (rs, rowNum) -> new Movement(
                    rs.getObject("id", UUID.class),
                    rs.getString("reference"),
                    rs.getString("origin"),
                    rs.getString("destination"),
                    rs.getTimestamp("planned_departure").toInstant(),
                    rs.getTimestamp("planned_arrival").toInstant(),
                    MovementStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()
            );

    @Override
    public void save(Movement movement) {
        Objects.requireNonNull(
                movement,
                "Movement is required"
        );

        jdbcTemplate.update("""
                INSERT INTO movements (
                    id,
                    reference,
                    origin,
                    destination,
                    planned_departure,
                    planned_arrival,
                    status,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                movement.id(),
                movement.reference(),
                movement.origin(),
                movement.destination(),
                Timestamp.from(movement.plannedDeparture()),
                Timestamp.from(movement.plannedArrival()),
                movement.status().name(),
                Timestamp.from(movement.createdAt()),
                Timestamp.from(movement.updatedAt())
        );
    }

    @Override
    public Optional<Movement> findById(UUID id) {
        Objects.requireNonNull(
                id,
                "Movement ID is required"
        );

        List<Movement> results = jdbcTemplate.query("""
                SELECT
                    id,
                    reference,
                    origin,
                    destination,
                    planned_departure,
                    planned_arrival,
                    status,
                    created_at,
                    updated_at
                FROM movements
                WHERE id = ?
                """,
                MOVEMENT_ROW_MAPPER,
                id
        );

        return results.stream().findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Movement> findByIdForUpdate(UUID id) {

        Objects.requireNonNull(
                id,
                "Movement ID is required"
        );

        List<Movement> results = jdbcTemplate.query("""
            SELECT
                id,
                reference,
                origin,
                destination,
                planned_departure,
                planned_arrival,
                status,
                created_at,
                updated_at
            FROM movements
            WHERE id = ?
            FOR UPDATE
            """,
                MOVEMENT_ROW_MAPPER,
                id
        );

        return results.stream().findFirst();
    }

    @Override
    public List<Movement> findAll() {
        return jdbcTemplate.query("""
                SELECT
                    id,
                    reference,
                    origin,
                    destination,
                    planned_departure,
                    planned_arrival,
                    status,
                    created_at,
                    updated_at
                FROM movements
                ORDER BY planned_departure, id
                """,
                MOVEMENT_ROW_MAPPER
        );
    }

    @Override
    public boolean existsByReference(String reference) {
        Objects.requireNonNull(
                reference,
                "Movement reference is required"
        );

        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM movements
                WHERE reference = ?
                """,
                Integer.class,
                reference
        );

        return count != null && count > 0;
    }


    @Override
    public boolean updateStatus(
            UUID movementId,
            MovementStatus expectedStatus,
            MovementStatus targetStatus,
            Instant updatedAt
    ) {
        Objects.requireNonNull(movementId);
        Objects.requireNonNull(expectedStatus);
        Objects.requireNonNull(targetStatus);
        Objects.requireNonNull(updatedAt);

        if (!expectedStatus.canTransitionTo(targetStatus)) {
            throw new IllegalArgumentException(
                    "Invalid movement transition: "
                            + expectedStatus
                            + " -> "
                            + targetStatus
            );
        }

        int updatedRows = jdbcTemplate.update(
                """
                UPDATE movements
                SET status = ?,
                    updated_at = ?
                WHERE id = ?
                  AND status = ?
                  AND updated_at <= ?
                """,
                targetStatus.name(),
                Timestamp.from(updatedAt),
                movementId,
                expectedStatus.name(),
                Timestamp.from(updatedAt)
        );

        return updatedRows == 1;
    }

}
