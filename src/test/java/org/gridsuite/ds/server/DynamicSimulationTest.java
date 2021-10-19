/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.StringTimeSeries;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.service.DynamicSimulationWorkerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient
@EnableWebFlux
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
    initializers = TestInitializer.class)
public class DynamicSimulationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    @MockBean
    private NetworkStoreService networkStoreClient;

    @SpyBean
    private DynamicSimulationWorkerService dynamicSimulationWorkerService;

    private FileSystem fileSystem;

    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String TEST_FILE = "IEEE14.iidm";
    private static final boolean RESULT = true;

    @Before
    public void init() throws IOException {
        //initialize in memory FS
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dynamicSimulationWorkerService.setFileSystem(fileSystem);

        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet("", TEST_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING))).willReturn(network);

        Map<String, TimeSeries> curves = new HashMap<>();
        TimeSeriesIndex index = new IrregularTimeSeriesIndex(new long[] {32, 64, 128, 256});
        curves.put("NETWORK__BUS____2-BUS____5-1_AC_iSide2", TimeSeries.createDouble("NETWORK__BUS____2-BUS____5-1_AC_iSide2", index, 333.847331, 333.847321, 333.847300, 333.847259));
        curves.put("NETWORK__BUS____1_TN_Upu_value", TimeSeries.createDouble("NETWORK__BUS____1_TN_Upu_value", index, 1.059970, 1.059970, 1.059970, 1.059970));

        index = new IrregularTimeSeriesIndex(new long[] {102479, 102479, 102479, 104396});
        StringTimeSeries timeLine = TimeSeries.createString("TimeLine", index,
                "CLA_2_5 - CLA : order to change topology",
                "_BUS____2-BUS____5-1_AC - LINE : opening both sides",
                "CLA_2_5 - CLA : order to change topology",
                "CLA_2_4 - CLA : arming by over-current constraint");

        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(RESULT, "", curves, timeLine)))
                .when(dynamicSimulationWorkerService).runAsync(any(), any(), any());

    }

    private static MockMultipartFile createMockMultipartFile(String fileName) throws IOException {
        try (InputStream inputStream = DynamicSimulationTest.class.getResourceAsStream("/" + fileName)) {
            return new MockMultipartFile("file", fileName, MediaType.TEXT_PLAIN_VALUE, inputStream);
        }
    }

    @Test
    public void test() {

        ClassPathResource dynamicModel = new ClassPathResource("dynamicModels.groovy");

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("dynamicModel", dynamicModel)
                .filename("dynamicModels.groovy");

        //run the dynamic simulation
        EntityExchangeResult<UUID> entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?startTime=0&stopTime=100", NETWORK_UUID_STRING)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        UUID runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        Message<byte[]> messageSwitch = output.receive(1000, "ds.result.destination");
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get("resultUuid").toString()));

        //get the calculation status
        EntityExchangeResult<String> entityExchangeResult2 = webTestClient.get()
                .uri("/v1/results/{resultUuid}/status", runUuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        //depending on the execution speed it can be both
        assertTrue(DynamicSimulationStatus.COMPLETED.name().equals(entityExchangeResult2.getResponseBody())
                || DynamicSimulationStatus.RUNNING.name().equals(entityExchangeResult2.getResponseBody()));

        //get the status of a non existing simulation and expect a not found
        webTestClient.get()
                .uri("/v1/results/{resultUuid}/status", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();

        //get the results of a non existing simulation and expect a not found
        webTestClient.get()
                .uri("/v1/results/{resultUuid}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();

        //get the results of the calculation and expect to get true since the calculation is complete
        webTestClient.get()
                .uri("/v1/results/{resultUuid}", runUuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody(boolean.class)
                .isEqualTo(RESULT);

        //delete a result and expect ok
        webTestClient.delete()
                .uri("/v1/results/{resultUuid}", runUuid)
                .exchange()
                .expectStatus().isOk();

        //try to get the removed result and except a not found
        webTestClient.get()
                .uri("/v1/results/{resultUuid}", runUuid)
                .exchange()
                .expectStatus().isNotFound();

        //delete all results and except ok
        webTestClient.delete()
                .uri("/v1/results")
                .exchange()
                .expectStatus().isOk();
    }

}
