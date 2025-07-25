/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.json.DynamicSimulationResultDeserializer;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesDataType;
import com.powsybl.timeseries.TimeSeriesMetadata;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.controller.utils.FileUtils;
import org.gridsuite.ds.server.controller.utils.ParameterUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.ParameterFile;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClientTest;
import org.gridsuite.ds.server.utils.Utils;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.messaging.Message;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.computation.service.NotificationService.HEADER_RESULT_UUID;
import static org.gridsuite.computation.service.NotificationService.HEADER_USER_ID;
import static org.gridsuite.ds.server.utils.Utils.RESOURCE_PATH_DELIMITER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationControllerIEEE14Test extends AbstractDynamicSimulationControllerTest {
    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMITER + "data" + RESOURCE_PATH_DELIMITER + "ieee14";
    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final String MAPPING_FILE = "mapping.json";
    public static final String MODELS_PAR = "models.par";
    public static final String RESULT_IDA_JSON = "result_IDA.json";
    public static final String RESULT_SIM_JSON = "result_SIM.json";

    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "22222222-0000-0000-0000-000000000000";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    // TODO remove when DynamicSimulationResultDeserializer correct curves by LinkedHashMap
    private static final Comparator<TimeSeries<?, ?>> TIME_SERIES_COMPARATOR = Comparator.comparing(timeSeries -> timeSeries.getMetadata().getName());

    private final Map<UUID, List<TimeSeries<?, ?>>> timeSeriesMockBd = new HashMap<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OutputDestination output;

    @Autowired
    private ObjectMapper objectMapper;

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
                              RESOURCE_PATH_DELIMITER + MAPPING_NAME_01 +
                              RESOURCE_PATH_DELIMITER + INPUT;

            // load models.par
            String parametersFilePath = inputDir + RESOURCE_PATH_DELIMITER + MODELS_PAR;
            InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
            byte[] parametersFileBytes;
            parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
            String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

            ParameterFile parameterFile = new ParameterFile(
                    MAPPING_NAME_01,
                    parametersFile);
            given(dynamicMappingClient.exportParameters(MAPPING_NAME_01)).willReturn(parameterFile);

            // load mapping.json
            String mappingPath = inputDir + RESOURCE_PATH_DELIMITER + MAPPING_FILE;
            InputMapping inputMapping = objectMapper.readValue(getClass().getResourceAsStream(mappingPath), InputMapping.class);
            given(dynamicMappingClient.getMapping(MAPPING_NAME_01)).willReturn(inputMapping);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
                timeSeriesMockBd.put(seriesUuid, (List<TimeSeries<?, ?>>) args[0]);
                return new TimeSeriesGroupInfos(seriesUuid);
            }
        ).when(timeSeriesClient).sendTimeSeries(any());
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();

        // delete all results
        mockMvc.perform(
                delete("/v1/results"))
            .andExpect(status().isOk());

        // clean fake time-series db
        timeSeriesMockBd.clear();
    }

    @Test
    public void test01GivenCurvesAndEvents() throws Exception {

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
        MvcResult result = mockMvc.perform(
                post("/v1/networks/{networkUuid}/run?" + "&mappingName=" + MAPPING_NAME_01, NETWORK_UUID_STRING)
                        .contentType(APPLICATION_JSON)
                        .header(HEADER_USER_ID, "testUserId")
                        .content(objectMapper.writeValueAsString(parameters)))
                                   .andExpect(status().isOk())
                                   .andReturn();

        UUID runUuid = objectMapper.readValue(result.getResponse().getContentAsString(), UUID.class);

        //TODO maybe find a more reliable way to test this : failed with 1000 * 30 timeout
        Message<byte[]> messageSwitch = output.receive(1000 * 40, dsResultDestination);
        assertThat(messageSwitch.getHeaders()).containsEntry(HEADER_RESULT_UUID, runUuid.toString());

        // --- CHECK result at abstract level --- //
        // expected seriesNames
        List<String> expectedSeriesNames = curveInfosList.stream().map(curveInfos -> curveInfos.getEquipmentId() + "_" + curveInfos.getVariableId()).toList();

        // get timeseries from mock timeseries db
        UUID timeSeriesUuid = TimeSeriesClientTest.TIME_SERIES_UUID;
        List<TimeSeries<?, ?>> resultTimeSeries = timeSeriesMockBd.get(timeSeriesUuid);
        // result seriesNames
        List<String> seriesNames = resultTimeSeries.stream().map(TimeSeries::getMetadata).map(TimeSeriesMetadata::getName).toList();

        // compare result only series' names
        expectedSeriesNames.forEach(expectedSeriesName -> {
            logger.info(String.format("Check time series %s exists or not : %b", expectedSeriesName, seriesNames.contains(expectedSeriesName)));
            assertThat(seriesNames).contains(expectedSeriesName);
        });

        // --- CHECK result at detail level --- //
        // prepare expected result to compare
        String outputDir = DATA_IEEE14_BASE_DIR +
                           RESOURCE_PATH_DELIMITER + MAPPING_NAME_01 +
                           RESOURCE_PATH_DELIMITER + OUTPUT;
        DynamicSimulationResult expectedResult = DynamicSimulationResultDeserializer.read(getClass().getResourceAsStream(outputDir + RESOURCE_PATH_DELIMITER + RESULT_SIM_JSON));
        List<TimeSeries<?, ?>> expectedTimeSeries = new ArrayList<>(expectedResult.getCurves().values());
        expectedTimeSeries.sort(TIME_SERIES_COMPARATOR);
        String jsonExpectedTimeSeries = TimeSeries.toJson(expectedTimeSeries);

        // convert result time series to json
        resultTimeSeries.sort(TIME_SERIES_COMPARATOR);
        String jsonResultTimeSeries = TimeSeries.toJson(resultTimeSeries);

        // export result to file
        FileUtils.writeBytesToFile(this, outputDir + RESOURCE_PATH_DELIMITER + "exported_" + RESULT_SIM_JSON, jsonResultTimeSeries.getBytes());

        // compare result only timeseries
        assertThat(objectMapper.readTree(jsonResultTimeSeries)).isEqualTo(objectMapper.readTree(jsonExpectedTimeSeries));

        // check dump file not empty
        result = mockMvc.perform(
                        get("/v1/results/{resultUuid}/output-state", runUuid))
                .andExpect(status().isOk())
                .andReturn();
        byte[] zippedOutputState = result.getResponse().getContentAsByteArray();

        assertThat(zippedOutputState)
                .withFailMessage("Expecting Output state of dynamic simulation to be not empty but was empty.")
                .isNotEmpty();
        logger.info("Size of zipped output state = {} KB ", zippedOutputState.length / 1024);

        // export dump file content in original and gzip formats to manual check
        FileUtils.writeBytesToFile(this, outputDir + RESOURCE_PATH_DELIMITER + "outputState.dmp.gz", zippedOutputState);
        File file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile() +
                             outputDir + RESOURCE_PATH_DELIMITER + "outputState.dmp");
        Utils.unzip(zippedOutputState, file.toPath());

        // check dynamic model persisted in result in gzip format not empty
        result = mockMvc.perform(
                        get("/v1/results/{resultUuid}/dynamic-model", runUuid))
                .andExpect(status().isOk())
                .andReturn();
        byte[] zippedDynamicModel = result.getResponse().getContentAsByteArray();

        assertThat(zippedDynamicModel)
                .withFailMessage("Expecting dynamic model of dynamic simulation to be not empty but was empty.")
                .isNotEmpty();
        logger.info("Size of zipped dynamic model = {} B ", zippedDynamicModel.length);

        // export dynamic model in json and dump files to manual check
        List<DynamicModelConfig> dynamicModel = Utils.unzip(zippedDynamicModel, objectMapper, new TypeReference<>() { });
        String jsonDynamicModel = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dynamicModel);
        FileUtils.writeBytesToFile(this, outputDir + RESOURCE_PATH_DELIMITER + "dynamicModel.json", jsonDynamicModel.getBytes());

        file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile() +
                 outputDir + RESOURCE_PATH_DELIMITER + "dynamicModel.dmp");
        Utils.unzip(zippedDynamicModel, file.toPath());

        // check parameters persisted in result in gzip format not empty
        result = mockMvc.perform(
                        get("/v1/results/{resultUuid}/parameters", runUuid))
                .andExpect(status().isOk())
                .andReturn();
        byte[] zippedDynamicSimulationParameters = result.getResponse().getContentAsByteArray();

        assertThat(zippedDynamicSimulationParameters)
                .withFailMessage("Expecting parameters of dynamic simulation to be not empty but was empty.")
                .isNotEmpty();
        logger.info("Size of zipped parameters = {} KB ", zippedDynamicSimulationParameters.length / 1024);

        // export dynamic model in json and dump files to manual check
        DynamicSimulationParameters dynamicSimulationParameters = Utils.unzip(zippedDynamicSimulationParameters, objectMapper, DynamicSimulationParameters.class);
        String jsonDynamicSimulationParameters = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dynamicSimulationParameters);
        FileUtils.writeBytesToFile(this, outputDir + RESOURCE_PATH_DELIMITER + "dynamicSimulationParameters.json", jsonDynamicSimulationParameters.getBytes());

        file = new File(Objects.requireNonNull(this.getClass().getClassLoader().getResource(".")).getFile() +
                        outputDir + RESOURCE_PATH_DELIMITER + "dynamicSimulationParameters.dmp");
        Utils.unzip(zippedDynamicSimulationParameters, file.toPath());

    }

    @Test
    public void testExportDynamicModel() throws Exception {
        //export the dynamic model on a specific variant
        MvcResult result = mockMvc.perform(
                        get("/v1/networks/{networkUuid}/export-dynamic-model?variantId=" +
                            VARIANT_1_ID + "&mappingName=" + MAPPING_NAME_01, NETWORK_UUID_STRING))
                .andExpect(status().isOk())
                .andReturn();
        List<DynamicModelConfig> dynamicModelConfigList = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() { });
        assertThat(dynamicModelConfigList).hasSize(21);
    }
}
