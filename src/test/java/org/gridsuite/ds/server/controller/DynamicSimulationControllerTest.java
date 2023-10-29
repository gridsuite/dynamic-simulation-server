/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.dynamicsimulation.DynamicSimulationResultImpl;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.StringTimeSeries;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.gridsuite.ds.server.controller.utils.ParameterUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClientTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.gridsuite.ds.server.service.contexts.DynamicSimulationFailedContext.HEADER_MESSAGE;
import static org.gridsuite.ds.server.service.contexts.DynamicSimulationFailedContext.HEADER_RESULT_UUID;
import static org.gridsuite.ds.server.service.notification.NotificationService.FAIL_MESSAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isNull;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationControllerTest extends AbstractDynamicSimulationControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    private static final String MAPPING_NAME = "IEEE14";
    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "22222222-0000-0000-0000-000000000000";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String TEST_FILE = "IEEE14.iidm";
    private static final boolean RESULT = true;

    @Override
    public OutputDestination getOutputDestination() {
        return output;
    }

    @Override
    protected void initNetworkStoreServiceMock() {
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet("", TEST_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
    }

    @Override
    protected void initDynamicMappingServiceMock() {
        Script scriptObj = new Script(
                MAPPING_NAME + "-script",
                MAPPING_NAME,
                "",
                new Date(),
                "");
        given(dynamicMappingClient.createFromMapping(MAPPING_NAME)).willReturn(Mono.just(scriptObj));
    }

    @Override
    protected void initTimeSeriesServiceMock() {
        given(timeSeriesClient.sendTimeSeries(any())).willReturn(Mono.just(new TimeSeriesGroupInfos(UUID.fromString(TimeSeriesClientTest.TIME_SERIES_UUID))));
        given(timeSeriesClient.deleteTimeSeriesGroup(any())).willReturn(Mono.empty());
    }

    @Before
    public void setUp() throws IOException {
        super.setUp();

        // mock DynamicSimulationWorkerService
        Map<String, TimeSeries> curves = new HashMap<>();
        TimeSeriesIndex index = new IrregularTimeSeriesIndex(new long[]{32, 64, 128, 256});
        curves.put("NETWORK__BUS____2-BUS____5-1_AC_iSide2", TimeSeries.createDouble("NETWORK__BUS____2-BUS____5-1_AC_iSide2", index, 333.847331, 333.847321, 333.847300, 333.847259));
        curves.put("NETWORK__BUS____1_TN_Upu_value", TimeSeries.createDouble("NETWORK__BUS____1_TN_Upu_value", index, 1.059970, 1.059970, 1.059970, 1.059970));

        index = new IrregularTimeSeriesIndex(new long[]{102479, 102479, 102479, 104396});
        StringTimeSeries timeLine = TimeSeries.createString("TimeLine", index,
                "CLA_2_5 - CLA : order to change topology",
                "_BUS____2-BUS____5-1_AC - LINE : opening both sides",
                "CLA_2_5 - CLA : order to change topology",
                "CLA_2_4 - CLA : arming by over-current constraint");

        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(RESULT, "", curves, timeLine)))
                .when(dynamicSimulationWorkerService).runAsync(any(), any(), any(), any(), any(), any(), any());
        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(RESULT, "", curves, timeLine)))
                .when(dynamicSimulationWorkerService).runAsync(any(), any(), isNull(), any(), any(), any(), any());
    }

    private static MockMultipartFile createMockMultipartFile(String fileName) throws IOException {
        try (InputStream inputStream = DynamicSimulationControllerTest.class.getResourceAsStream("/" + fileName)) {
            return new MockMultipartFile("file", fileName, MediaType.TEXT_PLAIN_VALUE, inputStream);
        }
    }

    @Test
    public void test() {

        // prepare parameters
        DynamicSimulationParametersInfos parameters = ParameterUtils.getDynamicSimulationParameters();

        //run the dynamic simulation on a specific variant
        EntityExchangeResult<UUID> entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?variantId=" + VARIANT_1_ID + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_STRING)
                .bodyValue(parameters)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        UUID runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        Message<byte[]> messageSwitch = output.receive(1000 * 6, dsResultDestination);
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(HEADER_RESULT_UUID).toString()));

        //run the dynamic simulation on the implicit default variant
        entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_STRING)
                .bodyValue(parameters)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        messageSwitch = output.receive(1000 * 6, dsResultDestination);
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(HEADER_RESULT_UUID).toString()));

        //get the calculation status
        EntityExchangeResult<DynamicSimulationStatus> entityExchangeResult2 = webTestClient.get()
                .uri("/v1/results/{resultUuid}/status", runUuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DynamicSimulationStatus.class)
                .returnResult();

        //depending on the execution speed it can be both
        assertTrue(DynamicSimulationStatus.CONVERGED == entityExchangeResult2.getResponseBody()
                || DynamicSimulationStatus.RUNNING == entityExchangeResult2.getResponseBody());

        //get the status of a non-existing simulation and expect a not found
        webTestClient.get()
                .uri("/v1/results/{resultUuid}/status", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();

        //get the results of a non-existing simulation and expect a not found
        webTestClient.get()
                .uri("/v1/results/{resultUuid}/timeseries", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();

        //get the result timeseries uuid of the calculation
        webTestClient.get()
                .uri("/v1/results/{resultUuid}/timeseries", runUuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class);

        //get the result timeline uuid of the calculation
        webTestClient.get()
                .uri("/v1/results/{resultUuid}/timeline", runUuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class);

        // get the ending status of the calculation which must be is converged
        webTestClient.get()
                .uri("/v1/results/{resultUuid}/status", runUuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DynamicSimulationStatus.class)
                .isEqualTo(DynamicSimulationStatus.CONVERGED);

        // test invalidate status => i.e. set NOT_DONE
        // set NOT_DONE
        webTestClient.put()
                .uri("/v1/results/invalidate-status?resultUuid=" + runUuid)
                .exchange()
                .expectStatus().isOk();
        // check whether NOT_DONE is persisted
        DynamicSimulationStatus statusAfterInvalidate = webTestClient.get()
                .uri("/v1/results/{resultUuid}/status", runUuid)
                .exchange()
                .expectStatus().isOk()
                .expectBody(DynamicSimulationStatus.class)
                .returnResult()
                .getResponseBody();
        assertEquals(DynamicSimulationStatus.NOT_DONE, statusAfterInvalidate);

        //delete a result and expect ok
        webTestClient.delete()
                .uri("/v1/results/{resultUuid}", runUuid)
                .exchange()
                .expectStatus().isOk();

        //try to get the removed result and except a not found
        webTestClient.get()
                .uri("/v1/results/{resultUuid}/timeseries", runUuid)
                .exchange()
                .expectStatus().isNotFound();

        //delete all results and except ok
        webTestClient.delete()
                .uri("/v1/results")
                .exchange()
                .expectStatus().isOk();

        // network not found
        entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_NOT_FOUND_STRING)
                .bodyValue(parameters)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        messageSwitch = output.receive(1000 * 5, dsFailedDestination);
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(HEADER_RESULT_UUID).toString()));
        assertEquals(FAIL_MESSAGE + " : " + HttpStatus.NOT_FOUND, messageSwitch.getHeaders().get(HEADER_MESSAGE));
    }
}
