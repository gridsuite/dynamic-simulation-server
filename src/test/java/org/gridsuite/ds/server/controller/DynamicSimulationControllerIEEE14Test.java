/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
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
import com.powsybl.dynamicsimulation.json.DynamicSimulationResultDeserializer;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesMetadata;
import org.gridsuite.ds.server.controller.utils.FileUtils;
import org.gridsuite.ds.server.controller.utils.ParameterUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClientTest;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClientTest;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.ds.server.service.contexts.DynamicSimulationFailedContext.HEADER_MESSAGE;
import static org.gridsuite.ds.server.service.contexts.DynamicSimulationFailedContext.HEADER_RESULT_UUID;
import static org.gridsuite.ds.server.service.notification.NotificationService.FAIL_MESSAGE;
import static org.gridsuite.ds.server.service.parameters.ParametersService.MODELS_PAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationControllerIEEE14Test extends AbstractDynamicSimulationControllerTest {
    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMETER + "data" + RESOURCE_PATH_DELIMETER + "ieee14";
    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final String MODELS_GROOVY = "models.groovy";
    public static final String RESULT_IDA_JSON = "result_IDA.json";
    public static final String RESULT_SIM_JSON = "result_SIM.json";

    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "22222222-0000-0000-0000-000000000000";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";
    private static final String TEST_EXCEPTION_MESSAGE = "Test exception";

    private final Map<UUID, List<TimeSeries>> timeSeriesMockBd = new HashMap<>();

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    @Autowired
    private ObjectMapper mapper = new ObjectMapper();

    private static final String FIXED_DATE = "01/01/2023";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public OutputDestination getOutputDestination() {
        return output;
    }

    @Override
    protected void initNetworkStoreServiceMock() {
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
    }

    @Override
    protected void initDynamicMappingServiceMock() {
        try {
            String inputDir = DATA_IEEE14_BASE_DIR +
                    RESOURCE_PATH_DELIMETER + MAPPING_NAME_01 +
                    RESOURCE_PATH_DELIMETER + INPUT;

            // load models.groovy
            String scriptPath = inputDir + RESOURCE_PATH_DELIMETER + MODELS_GROOVY;
            InputStream scriptIS = getClass().getResourceAsStream(scriptPath);
            byte[] scriptBytes;
            scriptBytes = StreamUtils.copyToByteArray(scriptIS);
            String script = new String(scriptBytes, StandardCharsets.UTF_8);

            // load models.par
            String parametersFilePath = inputDir + RESOURCE_PATH_DELIMETER + MODELS_PAR;
            InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
            byte[] parametersFileBytes;
            parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
            String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

            Script scriptObj = new Script(
                    MAPPING_NAME_01 + "-script",
                    MAPPING_NAME_01,
                    script,
                    dateFormat.parse(FIXED_DATE),
                    parametersFile);
            given(dynamicMappingClient.createFromMapping(DynamicMappingClientTest.MAPPING_NAME_01)).willReturn(Mono.just(scriptObj));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    protected void initTimeSeriesServiceMock() {
        Mockito.doAnswer(new Answer<Mono<TimeSeriesGroupInfos>>() {
            @Override
            public Mono<TimeSeriesGroupInfos> answer(final InvocationOnMock invocation) {
                final Object[] args = invocation.getArguments();
                List<TimeSeries> data = (List<TimeSeries>) args[0];
                UUID seriesUuid = null;
                if (!data.isEmpty()) {
                    switch (data.get(0).getMetadata().getDataType()) {
                        case STRING:
                            seriesUuid = UUID.fromString(TimeSeriesClientTest.TIME_LINE_UUID);
                            break;
                        default:
                            seriesUuid = UUID.fromString(TimeSeriesClientTest.TIME_SERIES_UUID);
                            break;
                    }
                    timeSeriesMockBd.put(seriesUuid, (List<TimeSeries>) args[0]);
                }
                return Mono.just(new TimeSeriesGroupInfos(seriesUuid));
            }
        }).when(timeSeriesClient).sendTimeSeries(any());
    }

    @Override
    public void tearDown() {
        super.tearDown();

        // delete all results
        webTestClient.delete()
                .uri("/v1/results")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void test01GivenCurvesAndEvents() throws IOException {
        String testBaseDir = MAPPING_NAME_01;

        // prepare parameters
        DynamicSimulationParametersInfos parameters = ParameterUtils.getDefaultDynamicSimulationParameters();

        // Test SIM solver (IDA solver will be ignored to test at moment due to the non-determinist on different OSs, Debian vs Ubuntu)
        parameters.setSolverId("SIM");

        // given curves
        List<CurveInfos> curveInfosList = ParameterUtils.getCurveInfosList();
        parameters.setCurves(curveInfosList);

        // given events
        List<EventInfos> eventInfosList = ParameterUtils.getEventInfosList();
        parameters.setEvents(eventInfosList);

        //run the dynamic simulation (on a specific variant with variantId=" + VARIANT_1_ID + ")
        EntityExchangeResult<UUID> entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME_01, NETWORK_UUID_STRING)
                .bodyValue(parameters)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        UUID runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        //TODO maybe find a more reliable way to test this : failed with 1000 * 20 timeout
        Message<byte[]> messageSwitch = output.receive(1000 * 30, dsResultDestination);
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(DynamicSimulationResultContext.HEADER_RESULT_UUID).toString()));

        // --- CHECK result at abstract level --- //
        // expected seriesNames
        List<String> expectedSeriesNames = curveInfosList.stream().map(curveInfos -> curveInfos.getEquipmentId() + "_" + curveInfos.getVariableId()).collect(Collectors.toList());

        // get timeseries from mock timeseries db
        UUID timeSeriesUuid = UUID.fromString(TimeSeriesClientTest.TIME_SERIES_UUID);
        List<TimeSeries> resultTimeSeries = timeSeriesMockBd.remove(timeSeriesUuid);
        // result seriesNames
        List<String> seriesNames = resultTimeSeries.stream().map(TimeSeries::getMetadata).map(TimeSeriesMetadata::getName).collect(Collectors.toList());

        // compare result only series' names
        expectedSeriesNames.forEach(expectedSeriesName -> {
            logger.info(String.format("Check time series %s exists or not : %b", expectedSeriesName, seriesNames.contains(expectedSeriesName)));
            assertTrue(seriesNames.contains(expectedSeriesName));
        });

        // --- CHECK result at detail level --- //
        // prepare expected result to compare
        String outputDir = DATA_IEEE14_BASE_DIR +
                           RESOURCE_PATH_DELIMETER + testBaseDir +
                           RESOURCE_PATH_DELIMETER + OUTPUT;
        DynamicSimulationResult expectedResult = DynamicSimulationResultDeserializer.read(getClass().getResourceAsStream(outputDir + RESOURCE_PATH_DELIMETER + RESULT_SIM_JSON));
        String jsonExpectedTimeSeries = TimeSeries.toJson(new ArrayList<>(expectedResult.getCurves().values()));

        // convert result time series to json
        String jsonResultTimeSeries = TimeSeries.toJson(resultTimeSeries);

        // export result to file
        FileUtils.writeStringToFile(this, outputDir + RESOURCE_PATH_DELIMETER + "exported_" + RESULT_SIM_JSON, jsonResultTimeSeries);

        // compare result only timeseries
        assertEquals(mapper.readTree(jsonExpectedTimeSeries), mapper.readTree(jsonResultTimeSeries));
    }

    @Test
    public void test01GivenRunWithException() {
        // setup spy bean
        doAnswer((InvocationOnMock invocation) -> {
            throw new RuntimeException(TEST_EXCEPTION_MESSAGE);
        }).
        when(dynamicSimulationWorkerService).runAsync(any(), any(), any(), any(), any(), any(), any());

        // prepare parameters
        DynamicSimulationParametersInfos parameters = ParameterUtils.getDefaultDynamicSimulationParameters();

        //run the dynamic simulation
        EntityExchangeResult<UUID> entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME_01, NETWORK_UUID_STRING)
                .bodyValue(parameters)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        UUID runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        // Message failed must be sent
        Message<byte[]> messageSwitch = output.receive(1000 * 5, dsFailedDestination);

        // check uuid and failed message
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(HEADER_RESULT_UUID).toString()));
        assertEquals(FAIL_MESSAGE + " : " + TEST_EXCEPTION_MESSAGE, messageSwitch.getHeaders().get(HEADER_MESSAGE));
    }
}
