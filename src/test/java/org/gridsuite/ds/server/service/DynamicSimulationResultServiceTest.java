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

    static Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationResultServiceTest.class);

    @Autowired
    ResultRepository resultRepository;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TimeSeriesClient timeSeriesClient;

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
        ResultEntity resultEntity = dynamicSimulationResultService.insertStatus(DynamicSimulationStatus.CONVERGED.name());

        assertThat(resultEntity.getId()).isNotNull();

        Optional<ResultEntity> insertedResultEntityOpt = resultRepository.findById(resultEntity.getId());
        LOGGER.info("Expected result status = " + DynamicSimulationStatus.CONVERGED);
        LOGGER.info("Actual inserted result status = " + insertedResultEntityOpt.get().getStatus());
        assertThat(insertedResultEntityOpt.get().getStatus()).isEqualTo(DynamicSimulationStatus.CONVERGED.name());

        // --- get status of the entity -- //
        DynamicSimulationStatus status = dynamicSimulationResultService.getStatus(resultEntity.getId());

        LOGGER.info("Expected result status = " + DynamicSimulationStatus.CONVERGED);
        LOGGER.info("Actual get result status = " + insertedResultEntityOpt.get().getStatus());
        assertThat(status).isEqualTo(DynamicSimulationStatus.CONVERGED);

        // --- update the entity --- //
        List<UUID> updatedResultUuids = dynamicSimulationResultService.updateStatus(List.of(resultEntity.getId()), DynamicSimulationStatus.NOT_DONE.name());

        Optional<ResultEntity> updatedResultEntityOpt = resultRepository.findById(updatedResultUuids.get(0));
        // status must be changed
        LOGGER.info("Expected result status = " + DynamicSimulationStatus.NOT_DONE);
        LOGGER.info("Actual updated result status = " + updatedResultEntityOpt.get().getStatus());
        assertThat(updatedResultEntityOpt.get().getStatus()).isEqualTo(DynamicSimulationStatus.NOT_DONE.name());

        // --- update the result with time-series and timeline --- //
        dynamicSimulationResultService.updateResult(
                resultEntity.getId(),
                List.of(mock(StoredDoubleTimeSeries.class)),
                List.of(mock(StringTimeSeries.class)),
                DynamicSimulationStatus.CONVERGED
        );

        // new uuids time-series and timeline must be inserted
        updatedResultEntityOpt = resultRepository.findById(resultEntity.getId());
        assertThat(updatedResultEntityOpt.get().getTimeSeriesId()).isNotNull();
        assertThat(updatedResultEntityOpt.get().getTimeLineId()).isNotNull();

        // --- update the result without time-series and timeline --- //
        dynamicSimulationResultService.updateResult(
                resultEntity.getId(),
                null,
                null,
                DynamicSimulationStatus.CONVERGED
        );
        // no uuids time-series and timeline
        updatedResultEntityOpt = resultRepository.findById(resultEntity.getId());
        assertThat(updatedResultEntityOpt.get().getTimeSeriesId()).isNull();
        assertThat(updatedResultEntityOpt.get().getTimeLineId()).isNull();

        // --- delete result --- //
        dynamicSimulationResultService.deleteResult(resultEntity.getId());

        Optional<ResultEntity> foundResultEntity = resultRepository.findById(resultEntity.getId());
        assertThat(foundResultEntity).isNotPresent();

        // --- delete all --- //
        resultRepository.saveAllAndFlush(List.of(
                new ResultEntity(null, null, null, DynamicSimulationStatus.RUNNING.name()),
                new ResultEntity(null, null, null, DynamicSimulationStatus.RUNNING.name())
        )).stream().map(ResultEntity::getId).toList();

        dynamicSimulationResultService.deleteResults();
        assertThat(resultRepository.findAll()).isEmpty();
    }
}
