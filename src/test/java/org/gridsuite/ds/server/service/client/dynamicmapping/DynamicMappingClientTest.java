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
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.dto.dynamicmapping.Parameter;
import org.gridsuite.ds.server.service.client.AbstractWireMockRestClientTest;
import org.gridsuite.ds.server.service.client.dynamicmapping.impl.DynamicMappingClientImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.CREATE_MAPPING_PARAMETER_ERROR;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.DYNAMIC_MAPPING_NOT_FOUND;
import static org.gridsuite.ds.server.service.client.RestClient.DELIMITER;
import static org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient.API_VERSION;
import static org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient.DYNAMIC_MAPPING_PARAMETER_CREATE_END_POINT;
import static org.gridsuite.ds.server.service.client.utils.UrlUtils.buildEndPointUrl;
import static org.gridsuite.ds.server.utils.Utils.RESOURCE_PATH_DELIMITER;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicMappingClientTest extends AbstractWireMockRestClientTest {

    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMITER + "data" + RESOURCE_PATH_DELIMITER + "ieee14";
    public static final String INPUT = "input";
    public static final String MODELS_PAR = "models.par";

    private DynamicMappingClient dynamicMappingClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String getEndpointUrl() {
        return buildEndPointUrl("", API_VERSION,
                DYNAMIC_MAPPING_PARAMETER_CREATE_END_POINT);
    }

    @Override
    public void setup() {
        super.setup();
        dynamicMappingClient = new DynamicMappingClientImpl(
                // use new WireMockServer(DYNAMIC_MAPPING_PORT) to test with local server if needed
                initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
                restTemplate,
                objectMapper);
    }

    @Test
    public void testCreateFromMapping() throws IOException {
        String mappingName = MAPPING_NAME_01;

        String inputDir = DATA_IEEE14_BASE_DIR +
                          RESOURCE_PATH_DELIMITER + mappingName +
                          RESOURCE_PATH_DELIMITER + INPUT;

        // load models.par
        String parametersFilePath = inputDir + RESOURCE_PATH_DELIMITER + MODELS_PAR;
        InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
        byte[] parametersFileBytes;
        parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
        String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

        Parameter parameterObj = new Parameter(
                mappingName,
                parametersFile);

        ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
        String parameterJson = ow.writeValueAsString(parameterObj);

        // mock response for GET parameters/from/<mappingName>
        String baseUrl = getEndpointUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl + DELIMITER + "{mappingName}"))
                .withPathParam("mappingName", equalTo(mappingName))
                .willReturn(WireMock.ok()
                        .withBody(parameterJson)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                ));

        Parameter createdParameter = dynamicMappingClient.createFromMapping(MAPPING_NAME_01);

        // check result
        // load models.par
        assertThat(createdParameter.parametersFile()).isEqualTo(parametersFile);
    }

    @Test
    public void testCreateFromMappingGivenNotFound() {
        String mappingName = MAPPING_NAME_01;

        // mock response for GET parameters/from/<mappingName>
        String baseUrl = getEndpointUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl + "{mappingName}"))
                .withPathParam("mappingName", equalTo(mappingName))
                .willReturn(WireMock.notFound()
                ));

        // test service
        DynamicSimulationException dynamicSimulationException = catchThrowableOfType(() -> dynamicMappingClient.createFromMapping(MAPPING_NAME_01),
                DynamicSimulationException.class);

        // check result
        assertThat(dynamicSimulationException.getType())
                .isEqualTo(DYNAMIC_MAPPING_NOT_FOUND);
    }

    @Test
    public void testCreateFromMappingGivenException() {
        String mappingName = MAPPING_NAME_01;

        // mock response for test case GET with url - /parameters/from/{mappingName}
        String baseUrl = getEndpointUrl();

        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl + DELIMITER + "{mappingName}"))
                .withPathParam("mappingName", equalTo(mappingName))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE)
                ));

        // test service
        DynamicSimulationException dynamicSimulationException = catchThrowableOfType(() -> dynamicMappingClient.createFromMapping(MAPPING_NAME_01),
                DynamicSimulationException.class);

        // check result
        assertThat(dynamicSimulationException.getType())
                .isEqualTo(CREATE_MAPPING_PARAMETER_ERROR);
        assertThat(dynamicSimulationException.getMessage())
                .isEqualTo(ERROR_MESSAGE);

    }

}
