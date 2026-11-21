
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.application.EquipmentCargoSpaceRepository;
import com.charron.api.fleet.domain.CargoSpaceSpecifications;
import com.charron.api.fleet.domain.CargoSpaceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcEquipmentCargoSpaceRepository
        implements EquipmentCargoSpaceRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcEquipmentCargoSpaceRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void insert(
            UUID id,
            UUID equipmentId,
            String name,
            CargoSpaceSpecifications specifications
    ) {
        Objects.requireNonNull(id, "Cargo space ID is required");
        Objects.requireNonNull(equipmentId, "Equipment ID is required");
        Objects.requireNonNull(
                specifications,
                "Cargo space specifications are required"
        );

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Cargo space name is required"
            );
        }

        jdbcTemplate.update("""
                INSERT INTO equipment_cargo_spaces (
                    id,
                    equipment_id,
                    name,
                    space_type,
                    usable_length_mm,
                    usable_width_mm,
                    usable_height_mm
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                id,
                equipmentId,
                name.trim(),
                specifications.type().name(),
                specifications.usableLengthMm(),
                specifications.usableWidthMm(),
                specifications.usableHeightMm()
        );
    }

    @Override
    public List<EquipmentCargoSpace> findByEquipmentId(
            UUID equipmentId
    ) {
        Objects.requireNonNull(
                equipmentId,
                "Equipment ID is required"
        );

        return jdbcTemplate.query("""
                SELECT
                    id,
                    equipment_id,
                    name,
                    space_type,
                    usable_length_mm,
                    usable_width_mm,
                    usable_height_mm
                FROM equipment_cargo_spaces
                WHERE equipment_id = ?
                ORDER BY name, id
                """,
                (rs, rowNum) -> new EquipmentCargoSpace(
                        rs.getObject("id", UUID.class),
                        rs.getObject("equipment_id", UUID.class),
                        rs.getString("name"),
                        new CargoSpaceSpecifications(
                                CargoSpaceType.valueOf(
                                        rs.getString("space_type")
                                ),
                                rs.getLong("usable_length_mm"),
                                rs.getLong("usable_width_mm"),
                                rs.getObject(
                                        "usable_height_mm",
                                        Long.class
                                )
                        )
                ),
                equipmentId
        );
    }


    @Override
    public Optional<EquipmentCargoSpace> findById(
            UUID cargoSpaceId
    ) {
        Objects.requireNonNull(
                cargoSpaceId,
                "Cargo space ID is required"
        );

        List<EquipmentCargoSpace> results = jdbcTemplate.query("""
            SELECT
                id,
                equipment_id,
                name,
                space_type,
                usable_length_mm,
                usable_width_mm,
                usable_height_mm
            FROM equipment_cargo_spaces
            WHERE id = ?
            """,
                (rs, rowNum) -> new EquipmentCargoSpace(
                        rs.getObject("id", UUID.class),
                        rs.getObject("equipment_id", UUID.class),
                        rs.getString("name"),
                        new CargoSpaceSpecifications(
                                CargoSpaceType.valueOf(
                                        rs.getString("space_type")
                                ),
                                rs.getLong("usable_length_mm"),
                                rs.getLong("usable_width_mm"),
                                rs.getObject(
                                        "usable_height_mm",
                                        Long.class
                                )
                        )
                ),
                cargoSpaceId
        );

        return results.stream().findFirst();
    }

}
