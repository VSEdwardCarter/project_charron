
package com.charron.api.integration.application;

import java.util.Map;
import java.util.Optional;

public interface ExternalSourceRecordRepository {

    void upsert(
            String sourceSystem,
            String entityType,
            String externalId,
            Map<String, Object> payload
    );

    Optional<Map<String, Object>> findPayload(
            String sourceSystem,
            String entityType,
            String externalId
    );

    long count(
            String sourceSystem,
            String entityType
    );
}
