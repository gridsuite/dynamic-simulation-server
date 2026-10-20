/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.ds.server.CustomApplicationContextInitializer;
import org.gridsuite.ds.server.DynamicSimulationApplication;
import org.gridsuite.ds.server.controller.utils.TestUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.repository.DynamicSimulationParametersRepository;
import org.gridsuite.ds.server.service.DynamicSimulationWorkerService;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
        initializers = CustomApplicationContextInitializer.class)
abstract class AbstractDynamicSimulationControllerTest extends AbstractDynawoTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final String dsDebugDestination = "ds.debug.destination";
    protected final String dsResultDestination = "ds.result.destination";
    protected final String dsStoppedDestination = "ds.stopped.destination";
    protected final String dsCancelFailedDestination = "ds.cancelfailed.destination";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected DynamicMappingClient dynamicMappingClient;

    @MockitoBean
    protected TimeSeriesClient timeSeriesClient;

    @MockitoBean
    protected NetworkStoreService networkStoreClient;

    @MockitoBean
    protected DynamicSimulationParametersRepository dynamicSimulationParametersRepository;

    @MockitoSpyBean
    protected DynamicSimulationWorkerService dynamicSimulationWorkerService;

    @BeforeEach
    @Override
    void setUp() throws IOException {
        super.setUp();

        // NetworkStoreService mock
        initNetworkStoreServiceMock();

        // DynamicMappingService mock
        initDynamicMappingServiceMock();

        // TimeSeriesService mock
        initTimeSeriesServiceMock();

        // DynamicSimulationWorkerService spy
        initDynamicSimulationWorkerServiceSpy();
    }

    @AfterEach
    @Override
    void tearDown() throws Exception {
        super.tearDown();

        OutputDestination output = getOutputDestination();
        List<String> destinations = List.of(dsDebugDestination, dsResultDestination, dsStoppedDestination, dsCancelFailedDestination);

        TestUtils.assertQueuesEmptyThenClear(destinations, output);
    }

    protected abstract OutputDestination getOutputDestination();

    protected abstract void initNetworkStoreServiceMock();

    protected abstract void initDynamicMappingServiceMock();

    protected abstract void initTimeSeriesServiceMock();

    private void initDynamicSimulationWorkerServiceSpy() {
        // setup spy bean
        when(dynamicSimulationWorkerService.getComputationManager()).thenReturn(computationManager);
    }

    // --- utility methods --- //
    protected void assertResultStatus(UUID runUuid, DynamicSimulationStatus expectedStatus) throws Exception {

        MvcResult result = mockMvc.perform(
                        get("/v1/results/{resultUuid}/status", runUuid))
                .andExpect(status().isOk()).andReturn();

        DynamicSimulationStatus status = null;
        if (!result.getResponse().getContentAsString().isEmpty()) {
            status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);
        }

        assertThat(status).isSameAs(expectedStatus);
    }

}
