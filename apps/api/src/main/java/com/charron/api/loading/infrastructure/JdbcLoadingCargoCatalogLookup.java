
package com.charron.api.loading.infrastructure;

import com.charron.api.loading.application.LoadingCargoCatalogLookup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLoadingCargoCatalogLookup
        implements LoadingCargoCatalogLookup {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoadingCargoCatalogLookup(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public Optional<CargoDetails> findById(UUID cargoId) {
        Objects.requireNonNull(cargoId, "Cargo ID is required");

        return jdbcTemplate.query(
                """
                SELECT
                    id,
                    name,
                    length_mm,
                    width_mm,
                    height_mm,
                    weight_grams
                FROM cargo
                WHERE id = ?
                """,
                (rs, rowNum) -> new CargoDetails(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        rs.getObject("length_mm", Long.class),
                        rs.getObject("width_mm", Long.class),
                        rs.getObject("height_mm", Long.class),
                        rs.getObject("weight_grams", Long.class)
                ),
                cargoId
        ).stream().findFirst();
    }
}
