
package com.charron.api.integration.infrastructure;

import com.charron.api.integration.application.ExternalSourceIngestionService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class OlistRelationalCsvAdapter {

    private final ExternalSourceIngestionService ingestionService;

    public OlistRelationalCsvAdapter(
            ExternalSourceIngestionService ingestionService
    ) {
        this.ingestionService = Objects.requireNonNull(ingestionService);
    }

    public int ingest(
            String entityType,
            Path csvPath,
            int limit
    ) throws IOException {

        Objects.requireNonNull(csvPath, "CSV path is required");

        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "Ingestion limit must be positive"
            );
        }

        Set<String> requiredColumns = switch (entityType) {
            case "order" -> Set.of("order_id", "customer_id");
            case "order_item" -> Set.of(
                    "order_id",
                    "order_item_id",
                    "product_id",
                    "seller_id"
            );
            case "customer" -> Set.of("customer_id");
            default -> throw new IllegalArgumentException(
                    "Unsupported entity type: " + entityType
            );
        };

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        int processed = 0;

        try (CSVParser parser = CSVParser.parse(
                csvPath.toFile(),
                StandardCharsets.UTF_8,
                format
        )) {

            if (!parser.getHeaderMap().keySet()
                    .containsAll(requiredColumns)) {
                throw new IllegalArgumentException(
                        "CSV missing required columns for "
                                + entityType + ": " + requiredColumns
                );
            }

            for (CSVRecord record : parser) {
                if (processed >= limit) {
                    break;
                }

                if (!record.isConsistent()) {
                    throw new IllegalArgumentException(
                            "Malformed CSV record "
                                    + record.getRecordNumber()
                    );
                }

                Map<String, Object> payload =
                        new LinkedHashMap<>(record.toMap());

                String externalId = switch (entityType) {
                    case "order" -> requiredValue(
                            payload, "order_id", record
                    );

                    case "customer" -> requiredValue(
                            payload, "customer_id", record
                    );

                    case "order_item" -> {
                        String orderId = requiredValue(
                                payload, "order_id", record
                        );
                        String itemId = requiredValue(
                                payload, "order_item_id", record
                        );

                        // Length-prefixed composite identity.
                        // Avoids ambiguous concatenation.
                        yield orderId.length() + ":" + orderId
                                + "|" + itemId.length() + ":" + itemId;
                    }

                    default -> throw new IllegalStateException(
                            "Unexpected entity type"
                    );
                };

                // Validate relationship identifiers when present.
                for (String column : requiredColumns) {
                    requiredValue(payload, column, record);
                }

                ingestionService.ingest(
                        "olist",
                        entityType,
                        externalId,
                        payload
                );

                processed++;
            }
        }

        return processed;
    }

    private static String requiredValue(
            Map<String, Object> payload,
            String column,
            CSVRecord record
    ) {
        Object value = payload.get(column);

        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing " + column
                            + " at CSV record "
                            + record.getRecordNumber()
            );
        }

        return text;
    }
}
