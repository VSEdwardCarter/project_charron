
package com.charron.api.integration.infrastructure;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

@Component
@ConditionalOnProperty(
        name = "charron.ingestion.olist.enabled",
        havingValue = "true"
)
public class OlistImportRunner implements ApplicationRunner {

    private final OlistProductCsvAdapter productAdapter;
    private final OlistSellerCsvAdapter sellerAdapter;
    private final OlistRelationalCsvAdapter relationalAdapter;

    public OlistImportRunner(
            OlistProductCsvAdapter productAdapter,
            OlistSellerCsvAdapter sellerAdapter,
            OlistRelationalCsvAdapter relationalAdapter
    ) {
        this.productAdapter = Objects.requireNonNull(productAdapter);
        this.sellerAdapter = Objects.requireNonNull(sellerAdapter);
        this.relationalAdapter = Objects.requireNonNull(relationalAdapter);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        String entityType = requiredOption(
                args, "charron.ingestion.olist.entity"
        );

        String path = requiredOption(
                args, "charron.ingestion.olist.path"
        );

        String limitValue = requiredOption(
                args, "charron.ingestion.olist.limit"
        );

        int limit = Integer.parseInt(limitValue);
        Path csvPath = Path.of(path);

        int processed = switch (entityType) {
            case "product" -> productAdapter.ingest(csvPath, limit);
            case "seller" -> sellerAdapter.ingest(csvPath, limit);

            case "order", "order_item", "customer" ->
                    relationalAdapter.ingest(
                            entityType,
                            csvPath,
                            limit
                    );

            default -> throw new IllegalArgumentException(
                    "Unsupported Olist entity type: " + entityType
            );
        };

        System.out.printf(
                "CHARRON Olist import completed: entity=%s, records=%d%n",
                entityType,
                processed
        );
    }

    private static String requiredOption(
            ApplicationArguments args,
            String name
    ) {
        var values = args.getOptionValues(name);

        if (values == null
                || values.size() != 1
                || values.getFirst().isBlank()) {
            throw new IllegalArgumentException(
                    "Required option missing: --" + name
            );
        }

        return values.getFirst();
    }
}
