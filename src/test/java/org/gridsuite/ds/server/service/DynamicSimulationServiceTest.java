/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.ds.server.DynamicSimulationApplication;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DynamicSimulationServiceTest {

    static Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationServiceTest.class);

    private static final String RESULT_UUID_STRING = "99999999-0000-0000-0000-000000000000";
    private static final UUID RESULT_UUID = UUID.fromString(RESULT_UUID_STRING);

    private static final String RESULT_UUID_2_STRING = "99999999-1111-0000-0000-000000000000";
    private static final UUID RESULT_UUID_2 = UUID.fromString(RESULT_UUID_2_STRING);

    @MockBean
    ResultRepository resultRepository;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    DynamicSimulationService dynamicSimulationService;

    @Test
    public void testUpdateStatusGivenOneResult() {

        // setup resultRepository mock
        List<ResultEntity> resultEntities = List.of(new ResultEntity(RESULT_UUID, null, null, DynamicSimulationStatus.CONVERGED.name()));
        List<UUID> resultUuids = List.of(RESULT_UUID);
        given(resultRepository.findAllById(resultUuids)).willReturn(resultEntities);
        given(resultRepository.saveAllAndFlush(resultEntities)).willReturn(resultEntities);

        // call method to be tested
        List<UUID> updatedResultUuids = dynamicSimulationService.updateStatus(resultUuids, DynamicSimulationStatus.NOT_DONE.name()).block();

        // check result
        // only one result
        LOGGER.info("Size of expected updatedResultUuids = " + 1);
        LOGGER.info("Size of actual updatedResultUuids = " + updatedResultUuids.size());
        assertEquals(1, updatedResultUuids.size());

        // the updated result must be identical to the expected one
        LOGGER.info("Expected result uuid = " + RESULT_UUID);
        LOGGER.info("Actual updated result uuid = " + updatedResultUuids.get(0));
        assertEquals(RESULT_UUID, updatedResultUuids.get(0));
    }

    @Test
    public void testUpdateStatusGivenTwoResults() throws JsonProcessingException {

        // setup resultRepository mock
        List<ResultEntity> resultEntities = List.of(
                new ResultEntity(RESULT_UUID, null, null, DynamicSimulationStatus.CONVERGED.name()),
                new ResultEntity(RESULT_UUID_2, null, null, DynamicSimulationStatus.CONVERGED.name())
                );
        List<UUID> resultUuids = List.of(RESULT_UUID, RESULT_UUID_2);
        given(resultRepository.findAllById(resultUuids)).willReturn(resultEntities);
        given(resultRepository.saveAllAndFlush(resultEntities)).willReturn(resultEntities);

        // call method to be tested
        List<UUID> updatedResultUuids = dynamicSimulationService.updateStatus(resultUuids, DynamicSimulationStatus.NOT_DONE.name()).block();

        // check result
        // only one result
        LOGGER.info("Size of expected updatedResultUuids = " + 2);
        LOGGER.info("Size of actual updatedResultUuids = " + updatedResultUuids.size());
        assertEquals(2, updatedResultUuids.size());

        // the updated result must be identical to the expected one
        String expectedResultUuidsJson = objectMapper.writeValueAsString(resultUuids);
        String actualUpdatedResultUuids = objectMapper.writeValueAsString(updatedResultUuids);
        LOGGER.info("Expected result uuids = " + expectedResultUuidsJson);
        LOGGER.info("Actual updated result uuids = " + actualUpdatedResultUuids);
        assertEquals(objectMapper.readTree(expectedResultUuidsJson), objectMapper.readTree(actualUpdatedResultUuids));
    }
}
