/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.json.DynamicSimulationResultDeserializer;
import com.powsybl.dynamicsimulation.json.DynamicSimulationResultSerializer;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.dynamicmapping.DynamicMappingServiceTest;
import org.gridsuite.ds.server.service.timeseries.TimeSeriesServiceTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.UUID;

import static org.gridsuite.ds.server.service.parameters.ParametersService.MODELS_PAR;
import static org.gridsuite.ds.server.service.timeseries.TimeSeriesService.UUID_KEY;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationControllerIEEE14Test extends AbstractDynamicSimulationControllerTest {
    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = "/data/ieee14";
    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final String MODELS_GROOVY = "models.groovy";
    public static final String RESULT_JSON = "result.json";

    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "22222222-0000-0000-0000-000000000000";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    @Override
    protected void initNetworkStoreServiceMock() throws IOException {
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
    }

    @Override
    protected void initDynamicMappingServiceMock() throws IOException {
        // load models.groovy
        String scriptPath = Paths.get(DATA_IEEE14_BASE_DIR, MAPPING_NAME_01, INPUT, MODELS_GROOVY).toString();
        InputStream scriptIS = getClass().getResourceAsStream(scriptPath);
        byte[] scriptBytes;
        scriptBytes = StreamUtils.copyToByteArray(scriptIS);
        String script = new String(scriptBytes, StandardCharsets.UTF_8);

        // load models.par
        String parametersFilePath = Paths.get(DATA_IEEE14_BASE_DIR, MAPPING_NAME_01, INPUT, MODELS_PAR).toString();
        InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
        byte[] parametersFileBytes;
        parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
        String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

        Script scriptObj =  new Script(
                MAPPING_NAME_01 + "-script",
                MAPPING_NAME_01,
                script,
                new Date(),
                true,
                parametersFile);
        given(dynamicMappingService.createFromMapping(DynamicMappingServiceTest.MAPPING_NAME_01)).willReturn(Mono.just(scriptObj));
    }

    @Override
    protected void initTimeSeriesServiceMock() throws IOException {
        given(timeSeriesService.sendTimeSeries(any())).willReturn(Mono.just(ImmutableMap.of(UUID_KEY, UUID.fromString(TimeSeriesServiceTest.TIME_SERIES_UUID))));
    }

    private String getResult(InputStream resultIS) throws IOException {
        DynamicSimulationResult result = DynamicSimulationResultDeserializer.read(resultIS);
        ByteArrayOutputStream bytesOS = new ByteArrayOutputStream();
        DynamicSimulationResultSerializer.write(result, bytesOS);
        String resultJson = bytesOS.toString();
        return resultJson;
    }

    private void writeResult(InputStream resultIS, Path jsonFile) throws IOException {
        DynamicSimulationResult result = DynamicSimulationResultDeserializer.read(resultIS);
        DynamicSimulationResultSerializer.write(result, jsonFile);
    }

    @Test
    public void test01() throws IOException {
        String testBaseDir = MAPPING_NAME_01;
        // load dynamic model file
        ClassPathResource dynamicModel = new ClassPathResource(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, MODELS_GROOVY).toString());
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("dynamicModel", dynamicModel)
                .filename(MODELS_GROOVY);

        //run the dynamic simulation (on a specific variant with variantId=" + VARIANT_1_ID + ")
        EntityExchangeResult<UUID> entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?&startTime=0&stopTime=50" + "&mappingName=" + MAPPING_NAME_01, NETWORK_UUID_STRING)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        UUID runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        Message<byte[]> messageSwitch = output.receive(1000 * 5, "ds.result.destination");
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(DynamicSimulationResultContext.RESULT_UUID).toString()));

        // prepare expected result to compare
        String jsonExpectedResult = getResult(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, OUTPUT, RESULT_JSON).toString()));

        // export result into file
        String resultDir = getClass().getResource(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, OUTPUT).toString()).getPath();
        Path resultJsonFile = Paths.get(resultDir).resolve("exported_" + RESULT_JSON);
        Files.deleteIfExists(resultJsonFile);
        Files.createFile(resultJsonFile);
        writeResult(new ByteArrayInputStream(messageSwitch.getPayload()), resultJsonFile);

        // get the result from exported file
        String jsonResult = getResult(Files.newInputStream(resultJsonFile));

        // compare result
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(jsonExpectedResult), mapper.readTree(jsonResult));
    }
}
