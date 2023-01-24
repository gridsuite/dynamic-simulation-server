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
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.gridsuite.ds.server.service.notification.NotificationService;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.doAnswer;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient(timeout = "PT360S")
@EnableWebFlux
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
        initializers = CustomApplicationContextInitializer.class)
public abstract class AbstractDynamicSimulationControllerTest {

    @MockBean
    protected DynamicMappingClient dynamicMappingClient;

    @MockBean
    protected TimeSeriesClient timeSeriesClient;

    @MockBean
    protected NetworkStoreService networkStoreClient;

    @SpyBean
    private NotificationService notificationService;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Before
    public void init() throws IOException {
        // NetworkStoreService mock
        initNetworkStoreServiceMock();

        // DynamicMappingService mock
        initDynamicMappingServiceMock();

        // TimeSeriesService mock
        initTimeSeriesServiceMock();

        // NotificationService mock
        initNotificationServiceMock();
    }

    protected abstract void initNetworkStoreServiceMock() throws IOException;

    protected abstract void initDynamicMappingServiceMock() throws IOException;

    protected abstract void initTimeSeriesServiceMock() throws IOException;

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
