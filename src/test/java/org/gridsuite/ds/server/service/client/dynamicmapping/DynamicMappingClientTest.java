/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client.dynamicmapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.assertj.core.api.Assertions;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.ParameterFile;
import org.gridsuite.ds.server.service.client.AbstractWireMockRestClientTest;
import org.gridsuite.ds.server.service.client.dynamicmapping.impl.DynamicMappingClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.ds.server.service.client.RestClient.URL_DELIMITER;
import static org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient.API_VERSION;
import static org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient.DYNAMIC_MAPPING_MAPPINGS_BASE_ENDPOINT;
import static org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient.DYNAMIC_MAPPING_PARAMETERS_EXPORT_ENDPOINT;
import static org.gridsuite.ds.server.service.client.utils.UrlUtils.buildEndPointUrl;
import static org.gridsuite.ds.server.utils.Utils.RESOURCE_PATH_DELIMITER;
import static org.gridsuite.ds.server.utils.assertions.Assertions.assertThat;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicMappingClientTest extends AbstractWireMockRestClientTest {

    // mapping names
    public static final String MAPPING_NAME = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMITER + "data" + RESOURCE_PATH_DELIMITER + "ieee14";
    public static final String INPUT = "input";
    public static final String MODELS_PAR = "models.par";
    public static final String MAPPING_JSON = "mapping.json";

    private DynamicMappingClient dynamicMappingClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getExportParametersBaseUrl() {
        return buildEndPointUrl("", API_VERSION,
                DYNAMIC_MAPPING_PARAMETERS_EXPORT_ENDPOINT);
    }

    private String getGetMappingBaseUrl() {
        return buildEndPointUrl("", API_VERSION,
                DYNAMIC_MAPPING_MAPPINGS_BASE_ENDPOINT);
    }

    @Override
    @BeforeEach
    public void setup() {
        super.setup();
        dynamicMappingClient = new DynamicMappingClientImpl(
                // use new WireMockServer(DYNAMIC_MAPPING_PORT) to test with local server if needed
                initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
                restTemplate,
                objectMapper);
    }

    @Test
    void testExportParameters() throws IOException {
        String mappingName = MAPPING_NAME;

        String inputDir = DATA_IEEE14_BASE_DIR +
                RESOURCE_PATH_DELIMITER + mappingName +
                RESOURCE_PATH_DELIMITER + INPUT;

        // load models.par
        String parametersFilePath = inputDir + RESOURCE_PATH_DELIMITER + MODELS_PAR;
        InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
        byte[] parametersFileBytes;
        parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
        String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

        ParameterFile parameterFile = new ParameterFile(
                mappingName,
                parametersFile);

        ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
        String parameterFileJson = ow.writeValueAsString(parameterFile);

        // mock response for GET parameters/export?mappingName=<mappingName>
        String baseUrl = getExportParametersBaseUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .withQueryParam("mappingName", equalTo(mappingName))
                .willReturn(WireMock.ok()
                        .withBody(parameterFileJson)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                ));

        ParameterFile createdParameterFile = dynamicMappingClient.exportParameters(MAPPING_NAME);

        // check result
        // load models.par
        assertThat(createdParameterFile.fileContent()).isEqualTo(parametersFile);
    }

    @Test
    void testExportParametersFromMappingNameGivenNotFound() {
        // mock response for GET parameters/export?mappingName=<mappingName>
        String baseUrl = getExportParametersBaseUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .withQueryParam("mappingName", equalTo(MAPPING_NAME))
                .willReturn(WireMock.notFound()
                ));

        // test service
        HttpClientErrorException dynamicSimulationException = catchThrowableOfType(HttpClientErrorException.class, () -> dynamicMappingClient.exportParameters(MAPPING_NAME));
        // check result
        Assertions.assertThat(dynamicSimulationException.getMessage()).contains(NOT_FOUND_ERROR_MESSAGE);
    }

    @Test
    void testExportParametersFromMappingNameGivenException() {
        // mock response for test case GET parameters/export?mappingName=<mappingName>
        String baseUrl = getExportParametersBaseUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl))
                .withQueryParam("mappingName", equalTo(MAPPING_NAME))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE)
                ));

        // test service
        HttpServerErrorException dynamicSimulationException = catchThrowableOfType(HttpServerErrorException.class, () -> dynamicMappingClient.exportParameters(MAPPING_NAME));
        // check result
        assertThat(dynamicSimulationException.getMessage()).contains(ERROR_MESSAGE);
    }

    @Test
    void testGetMapping() throws IOException {
        String mappingName = MAPPING_NAME;

        String inputDir = DATA_IEEE14_BASE_DIR +
                RESOURCE_PATH_DELIMITER + mappingName +
                RESOURCE_PATH_DELIMITER + INPUT;

        // load mapping.json to a string
        String mappingFilePath = inputDir + RESOURCE_PATH_DELIMITER + MAPPING_JSON;
        InputStream mappingFileIS = getClass().getResourceAsStream(mappingFilePath);
        byte[] mappingFileBytes;
        mappingFileBytes = StreamUtils.copyToByteArray(mappingFileIS);
        String mappingFileJson = new String(mappingFileBytes, StandardCharsets.UTF_8);

        // mock response for GET mappings/<mappingName>
        String baseUrl = getGetMappingBaseUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(baseUrl + URL_DELIMITER + mappingName))
                .willReturn(WireMock.ok()
                        .withBody(mappingFileJson)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                ));

        InputMapping resultMapping = dynamicMappingClient.getMapping(MAPPING_NAME);

        // check result
        // load mapping.json
        InputMapping expectedMapping = objectMapper.readValue(getClass().getResourceAsStream(mappingFilePath), InputMapping.class);
        assertThat(resultMapping).recursivelyEquals(expectedMapping);
    }

    @Test
    void testGetMappingFromMappingNameGivenNotFound() {
        // mock response for GET mappings/<mappingName>
        String baseUrl = getGetMappingBaseUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(baseUrl + URL_DELIMITER + MAPPING_NAME))
                .willReturn(WireMock.notFound()
                ));

        // test service
        HttpClientErrorException httpClientErrorException = catchThrowableOfType(HttpClientErrorException.class, () -> dynamicMappingClient.getMapping(MAPPING_NAME));
        Assertions.assertThat(httpClientErrorException.getMessage()).contains(NOT_FOUND_ERROR_MESSAGE);
    }

    @Test
    void testGetMappingFromMappingNameGivenException() {
        // mock response for test case GET mappings/<mappingName>
        String baseUrl = getGetMappingBaseUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo(baseUrl + URL_DELIMITER + MAPPING_NAME))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE)
                ));

        // test service
        HttpServerErrorException dynamicSimulationException = catchThrowableOfType(HttpServerErrorException.class, () -> dynamicMappingClient.getMapping(MAPPING_NAME));
        // check result
        assertThat(dynamicSimulationException.getMessage()).contains(ERROR_MESSAGE);
    }
}
