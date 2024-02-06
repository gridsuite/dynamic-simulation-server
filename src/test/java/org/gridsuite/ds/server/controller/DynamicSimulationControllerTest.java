/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.DynamicSimulationResultImpl;
import com.powsybl.dynamicsimulation.TimelineEvent;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.timeseries.DoubleTimeSeries;
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.gridsuite.ds.server.service.contexts.DynamicSimulationFailedContext.*;
import static org.gridsuite.ds.server.service.notification.NotificationService.FAIL_MESSAGE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationControllerTest extends AbstractDynamicSimulationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

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
        given(dynamicMappingClient.createFromMapping(MAPPING_NAME)).willReturn(scriptObj);
    }

    @Override
    protected void initTimeSeriesServiceMock() {
        given(timeSeriesClient.sendTimeSeries(any()))
            .willReturn(new TimeSeriesGroupInfos(UUID.fromString(TimeSeriesClientTest.TIME_SERIES_UUID)));
        doNothing().when(timeSeriesClient).deleteTimeSeriesGroup(any(UUID.class));
    }

    @Before
    public void setUp() throws IOException {
        super.setUp();

        // mock DynamicSimulationWorkerService
        Map<String, DoubleTimeSeries> curves = new HashMap<>();
        TimeSeriesIndex index = new IrregularTimeSeriesIndex(new long[]{32, 64, 128, 256});
        curves.put("NETWORK__BUS____2-BUS____5-1_AC_iSide2", TimeSeries.createDouble("NETWORK__BUS____2-BUS____5-1_AC_iSide2", index, 333.847331, 333.847321, 333.847300, 333.847259));
        curves.put("NETWORK__BUS____1_TN_Upu_value", TimeSeries.createDouble("NETWORK__BUS____1_TN_Upu_value", index, 1.059970, 1.059970, 1.059970, 1.059970));

        List<TimelineEvent> timeLine = List.of(
                new TimelineEvent(102479, "CLA_2_5 - CLA", "order to change topology"),
                new TimelineEvent(102479, "_BUS____2-BUS____5-1_AC - LINE", "opening both sides"),
                new TimelineEvent(102479, "CLA_2_5 - CLA", "order to change topology"),
                new TimelineEvent(104396, "CLA_2_4 - CLA", "arming by over-current constraint")
        );

        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(DynamicSimulationResult.Status.SUCCESS, "", curves, timeLine)))
                .when(dynamicSimulationWorkerService).runAsync(any(), any(), any(), any(), any(), any(), any());
        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(DynamicSimulationResult.Status.SUCCESS, "", curves, timeLine)))
                .when(dynamicSimulationWorkerService).runAsync(any(), any(), isNull(), any(), any(), any(), any());
    }

    private static MockMultipartFile createMockMultipartFile(String fileName) throws IOException {
        try (InputStream inputStream = DynamicSimulationControllerTest.class.getResourceAsStream("/" + fileName)) {
            return new MockMultipartFile("file", fileName, MediaType.TEXT_PLAIN_VALUE, inputStream);
        }
    }

    @Test
    public void test() throws Exception {

        // prepare parameters
        DynamicSimulationParametersInfos parameters = ParameterUtils.getDefaultDynamicSimulationParameters();

        //run the dynamic simulation on a specific variant
        MvcResult result = mockMvc.perform(
                post("/v1/networks/{networkUuid}/run?variantId=" +
                     VARIANT_1_ID + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_STRING, NETWORK_UUID_STRING)
                    .contentType(APPLICATION_JSON)
                    .header(HEADER_USER_ID, "testUserId")
                    .content(objectMapper.writeValueAsString(parameters)))
            .andExpect(status().isOk())
            .andReturn();
        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Message<byte[]> messageSwitch = output.receive(1000 * 10, dsResultDestination);
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(HEADER_RESULT_UUID).toString()));

        //run the dynamic simulation on the implicit default variant
        result = mockMvc.perform(
                post("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_STRING)
                    .contentType(APPLICATION_JSON)
                    .header(HEADER_USER_ID, "testUserId")
                    .content(objectMapper.writeValueAsString(parameters)))
            .andExpect(status().isOk())
            .andReturn();

        runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        messageSwitch = output.receive(1000 * 10, dsResultDestination);
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(HEADER_RESULT_UUID).toString()));

        //get the calculation status
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/status", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        DynamicSimulationStatus status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);

        //depending on the execution speed it can be both
        assertTrue(DynamicSimulationStatus.CONVERGED == status
                || DynamicSimulationStatus.RUNNING == status);

        //get the status of a non-existing simulation and expect a not found
        mockMvc.perform(
                get("/v1/results/{resultUuid}/status", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andReturn();

        //get the results of a non-existing simulation and expect a not found
        mockMvc.perform(
                get("/v1/results/{resultUuid}/timeseries", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andReturn();

        //get the result time-series uuid of the calculation
        mockMvc.perform(
                get("/v1/results/{resultUuid}/timeseries", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        //get the result timeline uuid of the calculation
        mockMvc.perform(
                get("/v1/results/{resultUuid}/timeline", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        // get the ending status of the calculation which must be is converged
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/status", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);

        assertSame(DynamicSimulationStatus.CONVERGED, status);

        // test invalidate status => i.e. set NOT_DONE
        // set NOT_DONE
        mockMvc.perform(
                put("/v1/results/invalidate-status?resultUuid=" + runUuid))
            .andExpect(status().isOk())
            .andReturn();

        // check whether NOT_DONE is persisted
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/status", runUuid))
            .andExpect(status().isOk())
            .andReturn();
        DynamicSimulationStatus statusAfterInvalidate = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);

        assertEquals(DynamicSimulationStatus.NOT_DONE, statusAfterInvalidate);

        //delete a result and expect ok
        mockMvc.perform(
                delete("/v1/results/{resultUuid}", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        //try to get the removed result and except a not found
        mockMvc.perform(
                get("/v1/results/{resultUuid}/timeseries", runUuid))
            .andExpect(status().isNotFound())
            .andReturn();

        //delete all results and except ok
        mockMvc.perform(
                delete("/v1/results"))
            .andExpect(status().isOk())
            .andReturn();

        // network not found
        result = mockMvc.perform(
                post("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_NOT_FOUND_STRING)
                        .contentType(APPLICATION_JSON)
                        .header(HEADER_USER_ID, "testUserId")
                        .content(objectMapper.writeValueAsString(parameters)))
            .andExpect(status().isOk())
            .andReturn();

        runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        messageSwitch = output.receive(1000 * 5, dsFailedDestination);
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(HEADER_RESULT_UUID).toString()));
        assertEquals(FAIL_MESSAGE + " : " + HttpStatus.NOT_FOUND, messageSwitch.getHeaders().get(HEADER_MESSAGE));
    }
}
