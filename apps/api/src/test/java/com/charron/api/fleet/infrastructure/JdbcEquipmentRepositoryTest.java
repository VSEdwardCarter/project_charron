
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;
import com.charron.api.fleet.application.EquipmentRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;


import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class JdbcEquipmentRepositoryTest {

    @Autowired
    private EquipmentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void insertsEquipmentIdentity() {
        UUID id = UUID.randomUUID();

        Equipment equipment = new Equipment(
                id,
                "CHARRON Test Tractor",
                EquipmentCategory.TRACTOR,
                null
        );

        repository.insertIdentity(equipment);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM equipment WHERE id = ?",
                Integer.class,
                id
        );

        assertEquals(1, count);
    }


    @Test
    void retrievesEquipmentIdentityById() {
        UUID id = UUID.randomUUID();

        Equipment original = new Equipment(
                id,
                "CHARRON Test Trailer",
                EquipmentCategory.TRAILER,
                "1HGCM82633A004352"
        );

        repository.insertIdentity(original);

        Equipment retrieved = repository.findIdentityById(id)
                .orElseThrow();

        assertEquals(original.getId(), retrieved.getId());
        assertEquals(original.getName(), retrieved.getName());
        assertEquals(original.getCategory(), retrieved.getCategory());
        assertEquals(original.getVin(), retrieved.getVin());
    }

    @Test
    void returnsEmptyWhenEquipmentDoesNotExist() {
        UUID nonexistentId = UUID.randomUUID();

        assertTrue(
                repository.findIdentityById(nonexistentId).isEmpty()
        );
    }

    @Test
    void retrievesEquipmentWithoutVin() {
        UUID id = UUID.randomUUID();

        Equipment original = new Equipment(
                id,
                "CHARRON Warehouse Forklift",
                EquipmentCategory.MATERIAL_HANDLING,
                null
        );

        repository.insertIdentity(original);

        Equipment retrieved = repository.findIdentityById(id)
                .orElseThrow();

        assertEquals(id, retrieved.getId());
        assertNull(retrieved.getVin());
    }

}
