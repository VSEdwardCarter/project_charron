
package com.charron.api.movement.infrastructure;

import com.charron.api.movement.application.MovementEquipmentAssignmentRepository;
import com.charron.api.movement.domain.MovementEquipmentAssignment;
import com.charron.api.movement.domain.MovementEquipmentRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcMovementEquipmentAssignmentRepository
        implements MovementEquipmentAssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MovementEquipmentAssignment> rowMapper =
            (rs, rowNum) -> new MovementEquipmentAssignment(
                    rs.getObject("id", UUID.class),
                    rs.getObject("movement_id", UUID.class),
                    rs.getObject("equipment_id", UUID.class),
                    MovementEquipmentRole.valueOf(
                            rs.getString("role")
                    ),
                    rs.getTimestamp("assigned_at").toInstant()
            );

    public JdbcMovementEquipmentAssignmentRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void insert(MovementEquipmentAssignment assignment) {
        Objects.requireNonNull(assignment);

        jdbcTemplate.update(
                """
                INSERT INTO movement_equipment_assignments (
                    id,
                    movement_id,
                    equipment_id,
                    role,
                    assigned_at
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                assignment.id(),
                assignment.movementId(),
                assignment.equipmentId(),
                assignment.role().name(),
                Timestamp.from(assignment.assignedAt())
        );
    }

    @Override
    public Optional<MovementEquipmentAssignment> findById(UUID id) {
        List<MovementEquipmentAssignment> results =
                jdbcTemplate.query(
                        """
                        SELECT
                            id,
                            movement_id,
                            equipment_id,
                            role,
                            assigned_at
                        FROM movement_equipment_assignments
                        WHERE id = ?
                        """,
                        rowMapper,
                        id
                );

        return results.stream().findFirst();
    }

    @Override
    public List<MovementEquipmentAssignment> findByMovementId(
            UUID movementId
    ) {
        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    movement_id,
                    equipment_id,
                    role,
                    assigned_at
                FROM movement_equipment_assignments
                WHERE movement_id = ?
                ORDER BY assigned_at, id
                """,
                rowMapper,
                movementId
        );
    }

    @Override
    public boolean existsByMovementAndEquipmentAndRole(
            UUID movementId,
            UUID equipmentId,
            MovementEquipmentRole role
    ) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM movement_equipment_assignments
                WHERE movement_id = ?
                  AND equipment_id = ?
                  AND role = ?
                """,
                Integer.class,
                movementId,
                equipmentId,
                role.name()
        );

        return count != null && count > 0;
    }
}
