
package com.charron.api.fleet.infrastructure;


import com.charron.api.fleet.domain.Equipment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.application.EquipmentRepository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;


import java.util.Objects;

@Repository
public class JdbcEquipmentRepository implements EquipmentRepository {


    private final JdbcTemplate jdbcTemplate;

    public JdbcEquipmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public void insertIdentity(Equipment equipment) {
        Objects.requireNonNull(
                equipment,
                "Equipment is required"
        );

        String sql = """
                INSERT INTO equipment (
                    id,
                    name,
                    category,
                    vin
                )
                VALUES (?, ?, ?, ?)
                """;

        int rows = jdbcTemplate.update(
                sql,
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory().name(),
                equipment.getVin()
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "Expected exactly one equipment row to be inserted"
            );
        }

    }

    @Override
    public Optional<Equipment> findIdentityById(UUID id) {
        Objects.requireNonNull(id, "Equipment ID is required");

        String sql = """
            SELECT id, name, category, vin
            FROM equipment
            WHERE id = ?
            """;

        List<Equipment> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new Equipment(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        EquipmentCategory.valueOf(
                                rs.getString("category")
                        ),
                        rs.getString("vin")
                ),
                id
        );

        return results.stream().findFirst();
    }

}
