
package com.charron.api.fleet.web;

import com.charron.api.fleet.application.EquipmentRepository;
import com.charron.api.fleet.domain.Equipment;
import com.charron.api.fleet.domain.EquipmentCategory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipmentControllerTest {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsEquipmentList() throws Exception {
        UUID equipmentId = UUID.randomUUID();

        equipmentRepository.insertIdentity(new Equipment(
                equipmentId,
                "CHARRON Test Tractor",
                EquipmentCategory.TRACTOR,
                null
        ));

        mockMvc.perform(get("/api/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[?(@.id == '" + equipmentId + "')].name"
                ).value("CHARRON Test Tractor"));
    }
}
