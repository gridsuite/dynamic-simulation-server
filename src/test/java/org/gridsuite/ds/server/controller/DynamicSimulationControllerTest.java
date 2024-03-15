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
import com.powsybl.timeseries.*;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.controller.utils.ParameterUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClientTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.ds.server.computation.service.NotificationService.*;
import static org.gridsuite.ds.server.controller.utils.TestUtils.assertType;
import static org.gridsuite.ds.server.service.DynamicSimulationService.COMPUTATION_TYPE;
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
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.ALL_COLLECTIONS_NEEDED_FOR_BUS_VIEW)).willReturn(network);
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
        Mockito.doAnswer(invocation -> {
                final Object[] args = invocation.getArguments();
                List<TimeSeries<?, ?>> data = (List<TimeSeries<?, ?>>) args[0];

                if (CollectionUtils.isEmpty(data)) {
                    return null;
                }

                UUID seriesUuid;
                if (Objects.requireNonNull(data.get(0).getMetadata().getDataType()) == TimeSeriesDataType.STRING) {
                    seriesUuid = TimeSeriesClientTest.TIME_LINE_UUID;
                } else {
                    seriesUuid = TimeSeriesClientTest.TIME_SERIES_UUID;
                }
                return new TimeSeriesGroupInfos(seriesUuid);
            }
        ).when(timeSeriesClient).sendTimeSeries(any());

        doNothing().when(timeSeriesClient).deleteTimeSeriesGroup(any(UUID.class));
    }

    @Before
    public void setUp() throws IOException {
        super.setUp();
    }

    @Test
    public void testGivenNotExistingNetworkUuid() throws Exception {

        // mock NetworkStoreService throws exception for a none-existing network uuid
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());

        // prepare parameters
        DynamicSimulationParametersInfos parameters = ParameterUtils.getDefaultDynamicSimulationParameters();

        // network not found
        MvcResult result = mockMvc.perform(
                        post("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME, NETWORK_UUID_NOT_FOUND_STRING)
                                .contentType(APPLICATION_JSON)
                                .header(HEADER_USER_ID, "testUserId")
                                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andReturn();

        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        Message<byte[]> messageSwitch = output.receive(1000 * 5, dsFailedDestination);
        assertThat(UUID.fromString(Objects.requireNonNull(messageSwitch.getHeaders().get(HEADER_RESULT_UUID)).toString())).isEqualTo(runUuid);
        assertThat(Objects.requireNonNull(messageSwitch.getHeaders().get(HEADER_MESSAGE)).toString()).contains(getFailedMessage(COMPUTATION_TYPE));
    }

    @Test
    public void testGivenTimeSeriesAndTimeLine() throws Exception {

        // mock DynamicSimulationWorkerService with time-series and timeline
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
                .when(dynamicSimulationWorkerService).getCompletableFuture(any(), any(), any(), any());
        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(DynamicSimulationResult.Status.SUCCESS, "", curves, timeLine)))
                .when(dynamicSimulationWorkerService).getCompletableFuture(any(), any(), isNull(), any());

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
            .andExpect(status().isNotFound());

        //get the time-series uuid of a non-existing simulation and expect a not found
        mockMvc.perform(
                get("/v1/results/{resultUuid}/timeseries", UUID.randomUUID()))
            .andExpect(status().isNotFound());

        //get the timeline uuid of a non-existing simulation and expect a not found
        mockMvc.perform(
                        get("/v1/results/{resultUuid}/timeline", UUID.randomUUID()))
                .andExpect(status().isNotFound());

        //get the result time-series uuid of the calculation
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/timeseries", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        // the return content must be a UUID class
        assertType(result.getResponse().getContentAsString(), UUID.class, objectMapper);

        //get the result timeline uuid of the calculation
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/timeline", runUuid))
            .andExpect(status().isOk())
            .andReturn();

        // the return content must be a UUID class
        assertType(result.getResponse().getContentAsString(), UUID.class, objectMapper);

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
            .andExpect(status().isOk());

        // check whether NOT_DONE is persisted
        result = mockMvc.perform(
                get("/v1/results/{resultUuid}/status", runUuid))
            .andExpect(status().isOk())
            .andReturn();
        DynamicSimulationStatus statusAfterInvalidate = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);

        assertEquals(DynamicSimulationStatus.NOT_DONE, statusAfterInvalidate);

        // set NOT_DONE for none existing result
        mockMvc.perform(
                        put("/v1/results/invalidate-status?resultUuid=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());

        //delete a result
        mockMvc.perform(
                delete("/v1/results/{resultUuid}", runUuid))
            .andExpect(status().isOk());

        //try to get the removed result and except a not found
        mockMvc.perform(
                get("/v1/results/{resultUuid}/timeseries", runUuid))
            .andExpect(status().isNotFound());

        //delete a none existing result
        mockMvc.perform(
                        delete("/v1/results/{resultUuid}", UUID.randomUUID()))
                .andExpect(status().isOk());

        //delete all results and except ok
        mockMvc.perform(
                delete("/v1/results"))
            .andExpect(status().isOk());
    }

    @Test
    public void testGivenEmptyTimeSeriesAndTimeLine() throws Exception {
        // mock DynamicSimulationWorkerService without time-series and timeline
        Map<String, DoubleTimeSeries> curves = new HashMap<>();
        List<TimelineEvent> timeLine = List.of();

        doReturn(CompletableFuture.completedFuture(new DynamicSimulationResultImpl(DynamicSimulationResult.Status.SUCCESS, "", curves, timeLine)))
                .when(dynamicSimulationWorkerService).getCompletableFuture(any(), any(), any(), any());

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

        // get the ending status of the calculation which must be is converged
        result = mockMvc.perform(
                        get("/v1/results/{resultUuid}/status", runUuid))
                .andExpect(status().isOk())
                .andReturn();

        DynamicSimulationStatus status = objectMapper.readValue(result.getResponse().getContentAsString(), DynamicSimulationStatus.class);

        assertSame(DynamicSimulationStatus.CONVERGED, status);

        //get time-series uuid of the calculation
        mockMvc.perform(
                        get("/v1/results/{resultUuid}/timeseries", runUuid))
                .andExpect(status().isNoContent());

        //get timeline uuid of the calculation
        mockMvc.perform(
                        get("/v1/results/{resultUuid}/timeline", runUuid))
                .andExpect(status().isNoContent());

    }
}
