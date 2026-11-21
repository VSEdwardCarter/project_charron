
package com.charron.api.integration.infrastructure;

import com.charron.api.integration.application.ExternalSourceRecordRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcExternalSourceRecordRepository
        implements ExternalSourceRecordRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public JdbcExternalSourceRecordRepository(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.jsonMapper = JsonMapper.builder().build();
    }

    @Override
    public void upsert(
            String sourceSystem,
            String entityType,
            String externalId,
            Map<String, Object> payload
    ) {
        validateIdentity(sourceSystem, entityType, externalId);
        Objects.requireNonNull(payload, "Source payload is required");

        String payloadJson = jsonMapper.writeValueAsString(payload);

        String sql = """
                INSERT INTO external_source_records (
                    id,
                    source_system,
                    entity_type,
                    external_id,
                    payload
                )
                VALUES (?, ?, ?, ?, CAST(? AS jsonb))
                ON CONFLICT (
                    source_system,
                    entity_type,
                    external_id
                )
                DO UPDATE SET
                    payload = EXCLUDED.payload,
                    last_ingested_at = CURRENT_TIMESTAMP
                WHERE external_source_records.payload
                    IS DISTINCT FROM EXCLUDED.payload
                """;

        jdbcTemplate.update(
                sql,
                UUID.randomUUID(),
                sourceSystem,
                entityType,
                externalId,
                payloadJson
        );
    }

    @Override
    public Optional<Map<String, Object>> findPayload(
            String sourceSystem,
            String entityType,
            String externalId
    ) {
        validateIdentity(sourceSystem, entityType, externalId);

        String sql = """
                SELECT payload::text
                FROM external_source_records
                WHERE source_system = ?
                  AND entity_type = ?
                  AND external_id = ?
                """;

        List<Map<String, Object>> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> jsonMapper.readValue(
                        rs.getString(1),
                        new TypeReference<Map<String, Object>>() {}
                ),
                sourceSystem,
                entityType,
                externalId
        );

        return results.stream().findFirst();
    }

    @Override
    public long count(
            String sourceSystem,
            String entityType
    ) {
        requireNonBlank(sourceSystem, "Source system");
        requireNonBlank(entityType, "Entity type");

        String sql = """
                SELECT COUNT(*)
                FROM external_source_records
                WHERE source_system = ?
                  AND entity_type = ?
                """;

        Long result = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                sourceSystem,
                entityType
        );

        return result == null ? 0L : result;
    }

    private static void validateIdentity(
            String sourceSystem,
            String entityType,
            String externalId
    ) {
        requireNonBlank(sourceSystem, "Source system");
        requireNonBlank(entityType, "Entity type");
        requireNonBlank(externalId, "External ID");
    }

    private static void requireNonBlank(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }
    }
}
