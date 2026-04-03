/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.ds.server.DynamicSimulationApplication;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.entities.parameters.DynamicSimulationParametersEntity;
import org.gridsuite.ds.server.repository.DynamicSimulationParametersRepository;
import org.gridsuite.ds.server.service.parameters.ParameterUtils;
import org.gridsuite.ds.server.utils.EquipmentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.computation.service.NotificationService.HEADER_USER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class})
class DynamicSimulationParametersControllerTest {

    private static final String USER_ID = "userId";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DynamicSimulationParametersRepository parametersRepository;

    @AfterEach
    void tearDown() {
        parametersRepository.deleteAll();
    }

    private DynamicSimulationParametersInfos newParametersInfos() {
        // Keep the DTO minimal and stable for CRUD tests
        DynamicSimulationParametersInfos infos = ParameterUtils.getDefaultParametersValues();
        infos.setCurves(List.of()); // make explicit to avoid null handling differences
        return infos;
    }

    private DynamicSimulationParametersInfos newParametersInfosWithCurves() {
        DynamicSimulationParametersInfos infos = ParameterUtils.getDefaultParametersValues();

        infos.setCurves(List.of(
             CurveInfos.builder()
                .equipmentType(EquipmentType.LOAD)
                .equipmentId("_LOAD___2_EC")
                .variableId("load_PPu")
                .build(),
            CurveInfos.builder()
                .equipmentType(EquipmentType.GENERATOR)
                .equipmentId("_GEN____3_SM")
                .variableId("generator_omegaPu")
                .build()
            ));

        return infos;
    }

    @Test
    void testCreateParameters() throws Exception {
        DynamicSimulationParametersInfos parametersInfos = newParametersInfos();

        MvcResult result = mockMvc.perform(post("/v1/parameters")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parametersInfos)))
                .andExpect(status().isOk())
                .andReturn();

        UUID parametersUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Optional<DynamicSimulationParametersEntity> entityOpt = parametersRepository.findById(parametersUuid);
        assertThat(entityOpt).isPresent();

        DynamicSimulationParametersInfos resultParametersInfos = entityOpt.get().toDto(true);

        assertThat(resultParametersInfos).usingRecursiveComparison().isEqualTo(parametersInfos);
    }

    @Test
    void testCreateDefaultParameters() throws Exception {
        DynamicSimulationParametersInfos defaultParametersInfos = newParametersInfos();

        MvcResult result = mockMvc.perform(post("/v1/parameters/default"))
                .andExpect(status().isOk())
                .andReturn();

        UUID parametersUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Optional<DynamicSimulationParametersEntity> entityOpt = parametersRepository.findById(parametersUuid);
        assertThat(entityOpt).isPresent();
        assertThat(entityOpt.get().getProvider()).isEqualTo("Dynawo");
        DynamicSimulationParametersInfos resultParametersInfos = entityOpt.get().toDto(true);
        assertThat(resultParametersInfos).usingRecursiveComparison().isEqualTo(defaultParametersInfos);
    }

    @Test
    void testDuplicateParameters() throws Exception {
        DynamicSimulationParametersInfos originalInfos = newParametersInfos();
        UUID originalUuid = parametersRepository.save(new DynamicSimulationParametersEntity(originalInfos)).getId();

        MvcResult result = mockMvc.perform(post("/v1/parameters")
                        .param("duplicateFrom", originalUuid.toString()))
                .andExpect(status().isOk())
                .andReturn();

        UUID duplicatedUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Optional<DynamicSimulationParametersEntity> duplicatedOpt = parametersRepository.findById(duplicatedUuid);
        assertThat(duplicatedOpt).isPresent();

        DynamicSimulationParametersInfos duplicatedInfos = duplicatedOpt.get().toDto(true);

        assertThat(duplicatedInfos).usingRecursiveComparison().isEqualTo(originalInfos);
    }

    @Test
    void testGetParametersWithCurves() throws Exception {
        // --- Setup: persist parameters with curves --- //
        DynamicSimulationParametersInfos infos = newParametersInfosWithCurves();
        UUID parametersUuid = parametersRepository.save(new DynamicSimulationParametersEntity(infos)).getId();

        // --- Execute --- //
        MvcResult result = mockMvc.perform(get("/v1/parameters/{uuid}", parametersUuid)
                        .header(HEADER_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andReturn();

        DynamicSimulationParametersInfos returned =
                objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationParametersInfos.class);

        // --- Verify --- //
        assertThat(returned.getId()).isEqualTo(parametersUuid);
        assertThat(returned.getCurves()).hasSize(2);
    }

    @Test
    void testGetAllParameters() throws Exception {
        DynamicSimulationParametersInfos infos = newParametersInfos();
        parametersRepository.saveAll(List.of(
                new DynamicSimulationParametersEntity(infos),
                new DynamicSimulationParametersEntity(infos)
        ));

        MvcResult result = mockMvc.perform(get("/v1/parameters"))
                .andExpect(status().isOk())
                .andReturn();

        List<DynamicSimulationParametersInfos> returned = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() { }
        );

        assertThat(returned).hasSize(2);
        assertThat(returned.get(0).getId()).isNotNull();
        assertThat(returned.get(1).getId()).isNotNull();
    }

    @Test
    void testUpdateParameters() throws Exception {
        DynamicSimulationParametersInfos infos = newParametersInfos();
        UUID parametersUuid = parametersRepository.save(new DynamicSimulationParametersEntity(infos)).getId();

        DynamicSimulationParametersInfos updatedInfos = newParametersInfos();
        updatedInfos.setStartTime(10d);
        updatedInfos.setStopTime(100d);
        updatedInfos.getNetwork().setCapacitorNoReclosingDelay(400); // change a field to ensure the update persists

        mockMvc.perform(put("/v1/parameters/{uuid}", parametersUuid)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedInfos)))
                .andExpect(status().isOk());

        Optional<DynamicSimulationParametersEntity> entityOpt = parametersRepository.findById(parametersUuid);
        assertThat(entityOpt).isPresent();

        DynamicSimulationParametersInfos persisted = entityOpt.get().toDto(false);
        assertThat(persisted.getStartTime()).isEqualTo(10);
        assertThat(persisted.getStopTime()).isEqualTo(100);
        assertThat(persisted.getNetwork().getCapacitorNoReclosingDelay()).isEqualTo(400);
    }

    @Test
    void testDeleteParameters() throws Exception {
        DynamicSimulationParametersInfos infos = newParametersInfos();
        UUID parametersUuid = parametersRepository.save(new DynamicSimulationParametersEntity(infos)).getId();

        mockMvc.perform(delete("/v1/parameters/{uuid}", parametersUuid))
                .andExpect(status().isOk());

        assertThat(parametersRepository.findById(parametersUuid)).isEmpty();
    }

    @Test
    void testGetProvider() throws Exception {
        DynamicSimulationParametersInfos infos = newParametersInfos();
        infos.setProvider("Dynawo");
        UUID parametersUuid = parametersRepository.save(new DynamicSimulationParametersEntity(infos)).getId();

        MvcResult result = mockMvc.perform(get("/v1/parameters/{uuid}/provider", parametersUuid))
                .andExpect(status().isOk())
                .andReturn();
        String provider = result.getResponse().getContentAsString();
        assertThat(provider).isEqualTo("Dynawo");
    }
}
