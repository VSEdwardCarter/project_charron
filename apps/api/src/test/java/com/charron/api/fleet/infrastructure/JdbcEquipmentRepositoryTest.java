
package com.charron.api.fleet.infrastructure;

import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class JdbcEquipmentRepositoryTest {

    @Autowired
    private JdbcEquipmentRepository repository;

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
}
