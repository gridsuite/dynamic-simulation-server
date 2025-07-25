/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.timeseries.StoredDoubleTimeSeries;
import com.powsybl.timeseries.StringTimeSeries;
import org.gridsuite.computation.service.UuidGeneratorService;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DynamicSimulationResultServiceTest {

    static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationResultServiceTest.class);

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TimeSeriesClient timeSeriesClient;

    @Autowired
    UuidGeneratorService uuidGeneratorService;

    @Autowired
    DynamicSimulationResultService dynamicSimulationResultService;

    @Before
    public void setUp() {
        when(timeSeriesClient.sendTimeSeries(anyList())).thenReturn(new TimeSeriesGroupInfos(UUID.randomUUID()));
    }

    @After
    public void cleanDB() {
        resultRepository.deleteAll();
    }

    @Test
    public void testCrud() {
        // --- insert an entity in the db --- //
        UUID entityUuid = uuidGeneratorService.generate();
        dynamicSimulationResultService.insertStatus(List.of(entityUuid), DynamicSimulationStatus.CONVERGED);

        Optional<ResultEntity> insertedResultEntityOpt = resultRepository.findById(entityUuid);
        LOGGER.info("Expected result status = " + DynamicSimulationStatus.CONVERGED);
        LOGGER.info("Actual inserted result status = " + insertedResultEntityOpt.get().getStatus());
        assertThat(insertedResultEntityOpt.get().getStatus()).isSameAs(DynamicSimulationStatus.CONVERGED);

        // --- get status of the entity -- //
        DynamicSimulationStatus status = dynamicSimulationResultService.findStatus(entityUuid);

        LOGGER.info("Expected result status = " + DynamicSimulationStatus.CONVERGED);
        LOGGER.info("Actual get result status = " + insertedResultEntityOpt.get().getStatus());
        assertThat(status).isEqualTo(DynamicSimulationStatus.CONVERGED);

        // --- update the entity --- //
        List<UUID> updatedResultUuids = dynamicSimulationResultService.updateStatus(List.of(entityUuid), DynamicSimulationStatus.NOT_DONE);

        Optional<ResultEntity> updatedResultEntityOpt = resultRepository.findById(updatedResultUuids.get(0));
        // status must be changed
        LOGGER.info("Expected result status = " + DynamicSimulationStatus.NOT_DONE);
        LOGGER.info("Actual updated result status = " + updatedResultEntityOpt.get().getStatus());
        assertThat(updatedResultEntityOpt.get().getStatus()).isSameAs(DynamicSimulationStatus.NOT_DONE);

        // --- update the result with time-series and timeline --- //
        dynamicSimulationResultService.updateResult(
                entityUuid,
                List.of(mock(StoredDoubleTimeSeries.class)),
                List.of(mock(StringTimeSeries.class)),
                DynamicSimulationStatus.CONVERGED,
                null,
                null,
                null
        );

        // new uuids time-series and timeline must be inserted
        updatedResultEntityOpt = resultRepository.findById(entityUuid);
        assertThat(updatedResultEntityOpt.get().getTimeSeriesId()).isNotNull();
        assertThat(updatedResultEntityOpt.get().getTimeLineId()).isNotNull();

        // --- update the result without time-series and timeline --- //
        dynamicSimulationResultService.updateResult(
                entityUuid,
                null,
                null,
                DynamicSimulationStatus.CONVERGED,
                null,
                null,
                null
        );
        // no uuids time-series and timeline
        updatedResultEntityOpt = resultRepository.findById(entityUuid);
        assertThat(updatedResultEntityOpt.get().getTimeSeriesId()).isNull();
        assertThat(updatedResultEntityOpt.get().getTimeLineId()).isNull();

        // --- delete result --- //
        dynamicSimulationResultService.delete(entityUuid);

        Optional<ResultEntity> foundResultEntity = resultRepository.findById(entityUuid);
        assertThat(foundResultEntity).isNotPresent();

        // --- delete all --- //
        resultRepository.saveAllAndFlush(List.of(
                new ResultEntity(uuidGeneratorService.generate(), null, null, DynamicSimulationStatus.RUNNING, null, null, null),
                new ResultEntity(uuidGeneratorService.generate(), null, null, DynamicSimulationStatus.RUNNING, null, null, null)
        )).stream().map(ResultEntity::getId).toList();

        dynamicSimulationResultService.deleteAll();
        assertThat(resultRepository.findAll()).isEmpty();
    }
}
