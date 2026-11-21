
package com.charron.api.integration.infrastructure;

import com.charron.api.integration.application.ExternalSourceRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OlistRelationalCsvAdapterTest {

    @Autowired
    private OlistRelationalCsvAdapter adapter;

    @Autowired
    private ExternalSourceRecordRepository repository;

    @TempDir
    Path tempDir;

    @Test
    void ingestsOrdersItemsAndCustomers() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();
        String productId = UUID.randomUUID().toString();
        String sellerId = UUID.randomUUID().toString();

        Path orders = writeCsv(
                "orders.csv",
                "order_id,customer_id,order_status\n"
                        + orderId + "," + customerId + ",delivered\n"
        );

        Path items = writeCsv(
                "items.csv",
                "order_id,order_item_id,product_id,seller_id,price\n"
                        + orderId + ",1," + productId + ","
                        + sellerId + ",58.90\n"
        );

        Path customers = writeCsv(
                "customers.csv",
                "customer_id,customer_city,customer_state\n"
                        + customerId + ",campinas,SP\n"
        );

        long initialOrders = repository.count("olist", "order");
        long initialItems = repository.count("olist", "order_item");
        long initialCustomers = repository.count("olist", "customer");

        assertEquals(1, adapter.ingest("order", orders, 5));
        assertEquals(1, adapter.ingest("order_item", items, 5));
        assertEquals(1, adapter.ingest("customer", customers, 5));

        assertEquals(
                initialOrders + 1,
                repository.count("olist", "order")
        );

        assertEquals(
                initialItems + 1,
                repository.count("olist", "order_item")
        );

        assertEquals(
                initialCustomers + 1,
                repository.count("olist", "customer")
        );

        Map<String, Object> order = repository.findPayload(
                "olist", "order", orderId
        ).orElseThrow();

        assertEquals("delivered", order.get("order_status"));
        assertEquals(customerId, order.get("customer_id"));

        String itemExternalId =
                orderId.length() + ":" + orderId + "|1:1";

        Map<String, Object> item = repository.findPayload(
                "olist", "order_item", itemExternalId
        ).orElseThrow();

        assertEquals(productId, item.get("product_id"));
        assertEquals(sellerId, item.get("seller_id"));
        assertEquals("58.90", item.get("price"));

        Map<String, Object> customer = repository.findPayload(
                "olist", "customer", customerId
        ).orElseThrow();

        assertEquals("campinas", customer.get("customer_city"));
    }

    @Test
    void replayDoesNotDuplicateOrders() throws Exception {
        String orderId = UUID.randomUUID().toString();
        String customerId = UUID.randomUUID().toString();

        Path csv = writeCsv(
                "replay.csv",
                "order_id,customer_id,order_status\n"
                        + orderId + "," + customerId + ",approved\n"
        );

        long initialCount = repository.count("olist", "order");

        assertEquals(1, adapter.ingest("order", csv, 5));
        assertEquals(1, adapter.ingest("order", csv, 5));

        assertEquals(
                initialCount + 1,
                repository.count("olist", "order")
        );
    }

    private Path writeCsv(
            String filename,
            String contents
    ) throws Exception {
        Path path = tempDir.resolve(filename);
        Files.writeString(path, contents);
        return path;
    }
}
