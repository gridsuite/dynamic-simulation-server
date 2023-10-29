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
import org.gridsuite.ds.server.service.DynamicSimulationWorkerService;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.gridsuite.ds.server.service.notification.NotificationService;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient(timeout = "PT360S")
@EnableWebFlux
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
        initializers = CustomApplicationContextInitializer.class)
public abstract class AbstractDynamicSimulationControllerTest extends AbstractDynawoTest {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final String dsResultDestination = "ds.result.destination";
    protected final String dsFailedDestination = "ds.failed.destination";

    public static final String RESOURCE_PATH_DELIMETER = "/";

    @MockBean
    protected DynamicMappingClient dynamicMappingClient;

    @MockBean
    protected TimeSeriesClient timeSeriesClient;

    @MockBean
    protected NetworkStoreService networkStoreClient;

    @SpyBean
    private NotificationService notificationService;

    @SpyBean
    protected DynamicSimulationWorkerService dynamicSimulationWorkerService;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Before
    public void setUp() throws IOException {
        super.setUp();

        // NetworkStoreService mock
        initNetworkStoreServiceMock();

        // DynamicMappingService mock
        initDynamicMappingServiceMock();

        // TimeSeriesService mock
        initTimeSeriesServiceMock();

        // NotificationService mock
        initNotificationServiceMock();

        // DynamicSimulationWorkerService spy
        initDynamicSimulationWorkerServiceSpy();
    }

    @After
    public void tearDown() {
        super.tearDown();

        OutputDestination output = getOutputDestination();
        List<String> destinations = List.of(dsFailedDestination, dsResultDestination);

        try {
            destinations.forEach(destination -> assertNull("Should not be any messages in queue " + destination + " : ", output.receive(1000 * 10, destination)));
        } finally {
            // purge in order to not fail the other tests
            output.clear();
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

    protected void initNotificationServiceMock() {
        // Emit messages in separate threads, like in production.
        // Otherwise the test binder calls consumers directly in the caller thread.
        // By coincidence, this leads to the following exception,
        // because we use webflux for the controller (calller thread),
        // and we use webflux to implement control flow in consumeRun
        // > Exception in consumeRun java.lang.IllegalStateException: block()/blockFirst()/blockLast() are blocking, which is not supported in thread parallel-5
        doAnswer((InvocationOnMock invocation) ->
                executorService.submit(() -> {
                    try {
                        return invocation.callRealMethod();
                    } catch (Throwable e) {
                        throw new RuntimeException("Error in test wrapping emit", e);
                    }
                })
        ).when(notificationService).emitRunDynamicSimulationMessage(Mockito.any());
    }

}
