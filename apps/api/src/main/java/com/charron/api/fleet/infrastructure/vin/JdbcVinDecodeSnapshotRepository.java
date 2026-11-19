
package com.charron.api.fleet.infrastructure.vin;

import com.charron.api.fleet.application.VinDecodeSnapshot;
import com.charron.api.fleet.application.VinDecodeSnapshotRepository;
import com.charron.api.fleet.domain.DecodedVehicle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcVinDecodeSnapshotRepository
        implements VinDecodeSnapshotRepository {

    private static final String DECODER_SOURCE =
            "NHTSA_VPIC_LOCAL";

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;
    private final String decoderVersion;

    public JdbcVinDecodeSnapshotRepository(
            @Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate,
            JsonMapper jsonMapper,
            @Value("${charron.vpic.dataset-version:unknown}")
            String decoderVersion
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.jsonMapper = Objects.requireNonNull(jsonMapper);
        this.decoderVersion = decoderVersion;
    }

    @Override
    public void save(
            UUID equipmentId,
            VinDecodeSnapshot snapshot
    ) {
        Objects.requireNonNull(
                equipmentId,
                "Equipment ID is required"
        );
        Objects.requireNonNull(
                snapshot,
                "VIN snapshot is required"
        );
        Objects.requireNonNull(
                snapshot.decodedVehicle(),
                "Decoded vehicle is required"
        );
        Objects.requireNonNull(
                snapshot.rawResponse(),
                "Raw VIN response is required"
        );

        String normalizedJson = jsonMapper.writeValueAsString(
                snapshot.decodedVehicle()
        );

        String rawJson = jsonMapper.writeValueAsString(
                prepareRawResponse(snapshot.rawResponse())
        );

        String sql = """
                INSERT INTO equipment_vin_decodes (
                    id,
                    equipment_id,
                    vin,
                    decoder_source,
                    decoder_version,
                    decoded_vehicle,
                    raw_response
                )
                VALUES (
                    ?, ?, ?, ?, ?,
                    CAST(? AS jsonb),
                    CAST(? AS jsonb)
                )
                """;

        int rows = jdbcTemplate.update(
                sql,
                UUID.randomUUID(),
                equipmentId,
                snapshot.decodedVehicle().vin(),
                DECODER_SOURCE,
                decoderVersion,
                normalizedJson,
                rawJson
        );

        if (rows != 1) {
            throw new IllegalStateException(
                    "Expected exactly one VIN decode snapshot row"
            );
        }
    }

    @Override
    public Optional<VinDecodeSnapshot> findLatestByEquipmentId(
            UUID equipmentId
    ) {
        Objects.requireNonNull(
                equipmentId,
                "Equipment ID is required"
        );

        String sql = """
                SELECT
                    decoded_vehicle::text AS decoded_vehicle,
                    raw_response::text AS raw_response
                FROM equipment_vin_decodes
                WHERE equipment_id = ?
                ORDER BY decoded_at DESC, id DESC
                LIMIT 1
                """;

        return jdbcTemplate.query(
                sql,
                rs -> {
                    if (!rs.next()) {
                        return Optional.empty();
                    }

                    DecodedVehicle vehicle = jsonMapper.readValue(
                            rs.getString("decoded_vehicle"),
                            DecodedVehicle.class
                    );

                    List<Map<String, Object>> rawResponse =
                            jsonMapper.readValue(
                                    rs.getString("raw_response"),
                                    new TypeReference<
                                            List<Map<String, Object>>
                                            >() {}
                            );

                    return Optional.of(
                            new VinDecodeSnapshot(
                                    vehicle,
                                    rawResponse
                            )
                    );
                },
                equipmentId
        );
    }

    private static List<Map<String, Object>> prepareRawResponse(
            List<Map<String, Object>> rawResponse
    ) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rawResponse) {
            Map<String, Object> converted = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Timestamp timestamp) {
                    value = timestamp.toLocalDateTime().toString();
                }

                converted.put(entry.getKey(), value);
            }

            result.add(converted);
        }

        return result;
    }
}
