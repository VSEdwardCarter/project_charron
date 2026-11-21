
package com.charron.api.integration.application;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class ExternalSourceIngestionService {

    private final ExternalSourceRecordRepository repository;

    public ExternalSourceIngestionService(
            ExternalSourceRecordRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    public void ingest(
            String sourceSystem,
            String entityType,
            String externalId,
            Map<String, Object> payload
    ) {
        requireNonBlank(sourceSystem, "Source system");
        requireNonBlank(entityType, "Entity type");
        requireNonBlank(externalId, "External ID");
        Objects.requireNonNull(payload, "Payload is required");

        repository.upsert(
                sourceSystem,
                entityType,
                externalId,
                payload
        );
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
