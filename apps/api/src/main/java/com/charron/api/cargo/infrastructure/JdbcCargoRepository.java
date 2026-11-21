
package com.charron.api.cargo.infrastructure;

import com.charron.api.cargo.application.CargoRepository;
import com.charron.api.cargo.domain.Cargo;
import com.charron.api.cargo.domain.CargoMeasurements;
import com.charron.api.common.measurement.Dimensions;
import com.charron.api.common.measurement.Weight;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCargoRepository implements CargoRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCargoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(
                jdbcTemplate,
                "JdbcTemplate is required"
        );
    }

    private static final RowMapper<Cargo> CARGO_ROW_MAPPER =
            (rs, rowNum) -> {

                Cargo cargo = new Cargo(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getString("reference"),
                        rs.getString("description")
                );

                Long weightGrams =
                        rs.getObject("weight_grams", Long.class);

                Long lengthMm =
                        rs.getObject("length_mm", Long.class);

                Long widthMm =
                        rs.getObject("width_mm", Long.class);

                Long heightMm =
                        rs.getObject("height_mm", Long.class);

                Weight weight = weightGrams == null
                        ? null
                        : new Weight(weightGrams);

                Dimensions dimensions = null;

                if (lengthMm != null
                        && widthMm != null
                        && heightMm != null) {

                    dimensions = new Dimensions(
                            lengthMm,
                            widthMm,
                            heightMm
                    );
                }

                if (weight != null || dimensions != null) {
                    cargo.setMeasurements(
                            new CargoMeasurements(
                                    weight,
                                    dimensions
                            )
                    );
                }

                return cargo;
            };

    @Override
    public void insert(Cargo cargo) {
        Objects.requireNonNull(
                cargo,
                "Cargo is required"
        );

        CargoMeasurements measurements =
                cargo.getMeasurements();

        Long weightGrams = null;
        Long lengthMm = null;
        Long widthMm = null;
        Long heightMm = null;

        if (measurements != null) {

            if (measurements.weight() != null) {
                weightGrams = measurements.weight().grams();

                if (weightGrams <= 0) {
                    throw new IllegalArgumentException(
                            "Cargo weight must be greater than zero"
                    );
                }
            }

            if (measurements.dimensions() != null) {
                Dimensions dimensions =
                        measurements.dimensions();

                lengthMm = dimensions.lengthMm();
                widthMm = dimensions.widthMm();
                heightMm = dimensions.heightMm();
            }
        }

        jdbcTemplate.update("""
                INSERT INTO cargo (
                    id,
                    name,
                    reference,
                    description,
                    weight_grams,
                    length_mm,
                    width_mm,
                    height_mm
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                cargo.getId(),
                cargo.getName(),
                cargo.getReference(),
                cargo.getDescription(),
                weightGrams,
                lengthMm,
                widthMm,
                heightMm
        );
    }

    @Override
    public Optional<Cargo> findById(UUID cargoId) {
        Objects.requireNonNull(
                cargoId,
                "Cargo ID is required"
        );

        List<Cargo> results = jdbcTemplate.query("""
                SELECT
                    id,
                    name,
                    reference,
                    description,
                    weight_grams,
                    length_mm,
                    width_mm,
                    height_mm
                FROM cargo
                WHERE id = ?
                """,
                CARGO_ROW_MAPPER,
                cargoId
        );

        return results.stream().findFirst();
    }

    @Override
    public List<Cargo> findAll() {
        return jdbcTemplate.query("""
                SELECT
                    id,
                    name,
                    reference,
                    description,
                    weight_grams,
                    length_mm,
                    width_mm,
                    height_mm
                FROM cargo
                ORDER BY name, id
                """,
                CARGO_ROW_MAPPER
        );
    }
}
