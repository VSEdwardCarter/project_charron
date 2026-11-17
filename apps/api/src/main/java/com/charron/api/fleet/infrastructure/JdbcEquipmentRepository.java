
package com.charron.api.fleet.infrastructure;


import com.charron.api.fleet.domain.Equipment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import com.charron.api.fleet.domain.EquipmentSpecifications;
import com.charron.api.fleet.domain.WeightSpecifications;



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
    public void insertSpecifications(
            UUID equipmentId,
            EquipmentSpecifications specifications
    ) {
        Objects.requireNonNull(
                equipmentId, "Equipment ID is required"
        );
        Objects.requireNonNull(
                specifications, "Equipment specifications are required"
        );

        Dimensions dimensions = specifications.exteriorDimensions();
        WeightSpecifications weights = specifications.weightSpecifications();

        String sql = """
            INSERT INTO equipment_specifications (
                equipment_id,
                length_mm,
                width_mm,
                height_mm,
                weight_specifications_present,
                empty_weight_grams,
                gross_weight_rating_grams,
                rated_payload_capacity_grams
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        int rows = jdbcTemplate.update(
                sql,
                equipmentId,
                dimensions == null ? null : dimensions.lengthMm(),
                dimensions == null ? null : dimensions.widthMm(),
                dimensions == null ? null : dimensions.heightMm(),
                weights != null,
                weights == null ? null : gramsOrNull(weights.emptyWeight()),
                weights == null ? null : gramsOrNull(weights.grossWeightRating()),
                weights == null ? null : gramsOrNull(weights.ratedPayloadCapacity())
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "Expected exactly one equipment specifications row"
            );
        }
    }

    private static Long gramsOrNull(Weight weight) {
        return weight == null ? null : weight.grams();
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


    @Override
    public Optional<EquipmentSpecifications> findSpecificationsByEquipmentId(
            UUID equipmentId
    ) {
        Objects.requireNonNull(
                equipmentId, "Equipment ID is required"
        );

        String sql = """
            SELECT
                length_mm,
                width_mm,
                height_mm,
                weight_specifications_present,
                empty_weight_grams,
                gross_weight_rating_grams,
                rated_payload_capacity_grams
            FROM equipment_specifications
            WHERE equipment_id = ?
            """;

        List<EquipmentSpecifications> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    Long lengthMm = rs.getObject("length_mm", Long.class);
                    Long widthMm = rs.getObject("width_mm", Long.class);
                    Long heightMm = rs.getObject("height_mm", Long.class);

                    Dimensions dimensions = lengthMm == null
                            ? null
                            : new Dimensions(lengthMm, widthMm, heightMm);

                    WeightSpecifications weights =
                            rs.getBoolean("weight_specifications_present")
                                    ? new WeightSpecifications(
                                    weightOrNull(rs.getObject(
                                            "empty_weight_grams", Long.class)),
                                    weightOrNull(rs.getObject(
                                            "gross_weight_rating_grams", Long.class)),
                                    weightOrNull(rs.getObject(
                                            "rated_payload_capacity_grams", Long.class))
                            )
                                    : null;

                    return new EquipmentSpecifications(dimensions, weights);
                },
                equipmentId
        );

        return results.stream().findFirst();
    }

    private static Weight weightOrNull(Long grams) {
        return grams == null ? null : new Weight(grams);
    }


}
