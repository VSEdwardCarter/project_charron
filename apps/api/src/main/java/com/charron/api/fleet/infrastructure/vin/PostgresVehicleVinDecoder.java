
package com.charron.api.fleet.infrastructure.vin;

import com.charron.api.fleet.application.VehicleVinDecoder;
import com.charron.api.fleet.application.VinDecodeSnapshot;
import com.charron.api.fleet.domain.DecodedVehicle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Repository
public class PostgresVehicleVinDecoder implements VehicleVinDecoder {

    private static final Pattern VIN_PATTERN =
            Pattern.compile("[A-HJ-NPR-Z0-9]{17}");

    private final JdbcTemplate jdbcTemplate;

    public PostgresVehicleVinDecoder(
            @Qualifier("vpicJdbcTemplate") JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
    }

    @Override
    public DecodedVehicle decode(String vin) {
        return decodeSnapshot(vin).decodedVehicle();
    }

    @Override
    public VinDecodeSnapshot decodeSnapshot(String vin) {
        String normalizedVin = normalizeVin(vin);

        // Read the complete vPIC result once.
        List<Map<String, Object>> rows = decodeRaw(normalizedVin);

        // Build normalized attributes from the same result.
        Map<String, String> attributes = new HashMap<>();

        for (Map<String, Object> row : rows) {
            Object codeObject = row.get("code");
            Object valueObject = row.get("value");

            if (codeObject == null || valueObject == null) {
                continue;
            }

            String code = codeObject.toString();
            String value = normalize(valueObject.toString());

            if (value != null) {
                attributes.putIfAbsent(code, value);
            }
        }

        String errorCode = attributes.get("ErrorCode");

        if (!"0".equals(errorCode)) {
            throw new IllegalArgumentException(
                    "VIN could not be decoded cleanly: "
                            + attributes.getOrDefault(
                            "ErrorText",
                            "Unknown decoding error"
                    )
            );
        }

        DecodedVehicle decodedVehicle = new DecodedVehicle(
                normalizedVin,
                errorCode,
                attributes.get("ErrorText"),

                attributes.get("Manufacturer"),
                attributes.get("Make"),
                attributes.get("Model"),
                integerOrNull(attributes.get("ModelYear")),
                attributes.get("Trim"),
                attributes.get("VehicleType"),
                attributes.get("BodyClass"),

                attributes.get("EngineManufacturer"),
                attributes.get("EngineModel"),
                attributes.get("EngineConfiguration"),
                integerOrNull(attributes.get("EngineCylinders")),
                doubleOrNull(attributes.get("DisplacementL")),
                doubleOrNull(attributes.get("EngineHP")),
                attributes.get("FuelTypePrimary"),

                attributes.get("TransmissionStyle"),
                attributes.get("DriveType"),
                attributes.get("GVWR")
        );

        return new VinDecodeSnapshot(decodedVehicle, rows);
    }

    public List<Map<String, Object>> decodeRaw(String vin) {
        String normalizedVin = normalizeVin(vin);

        return jdbcTemplate.queryForList(
                "SELECT * FROM vpic.spVinDecode(?)",
                normalizedVin
        );
    }

    private static String normalizeVin(String vin) {
        if (vin == null) {
            throw new IllegalArgumentException("VIN is required");
        }

        String normalizedVin =
                vin.trim().toUpperCase(Locale.ROOT);

        if (!VIN_PATTERN.matcher(normalizedVin).matches()) {
            throw new IllegalArgumentException(
                    "VIN must contain 17 valid characters"
            );
        }

        return normalizedVin;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private static Integer integerOrNull(String value) {
        return value == null ? null : Integer.valueOf(value);
    }

    private static Double doubleOrNull(String value) {
        return value == null ? null : Double.valueOf(value);
    }
}
