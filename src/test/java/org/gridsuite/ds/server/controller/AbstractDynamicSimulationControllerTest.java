/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.ds.server.CustomApplicationContextInitializer;
import org.gridsuite.ds.server.DynamicSimulationApplication;
import org.gridsuite.ds.server.controller.utils.TestUtils;
import org.gridsuite.ds.server.service.DynamicSimulationWorkerService;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
        initializers = CustomApplicationContextInitializer.class)
public abstract class AbstractDynamicSimulationControllerTest extends AbstractDynawoTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final String dsResultDestination = "ds.result.destination";
    protected final String dsFailedDestination = "ds.failed.destination";
    protected final String dsStoppedDestination = "ds.stopped.destination";

    @MockBean
    protected DynamicMappingClient dynamicMappingClient;

    @MockBean
    protected TimeSeriesClient timeSeriesClient;

    @MockBean
    protected NetworkStoreService networkStoreClient;

    @SpyBean
    protected DynamicSimulationWorkerService dynamicSimulationWorkerService;

    @Before
    public void setUp() throws IOException {
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

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        OutputDestination output = getOutputDestination();
        List<String> destinations = List.of(dsFailedDestination, dsResultDestination, dsStoppedDestination);

        try {
            TestUtils.assertQueuesEmptyThenClear(destinations, output);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error while checking message queues empty", e);
        }
    }

    protected abstract OutputDestination getOutputDestination();

    protected abstract void initNetworkStoreServiceMock();

    protected abstract void initDynamicMappingServiceMock();

    protected abstract void initTimeSeriesServiceMock();

    private void initDynamicSimulationWorkerServiceSpy() {
        // setup spy bean
        when(dynamicSimulationWorkerService.getComputationManager()).thenReturn(computationManager);
    }

}
