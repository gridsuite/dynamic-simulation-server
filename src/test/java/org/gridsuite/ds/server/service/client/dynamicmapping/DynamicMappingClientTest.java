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
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.client.AbstractWireMockRestClientTest;
import org.gridsuite.ds.server.service.client.dynamicmapping.impl.DynamicMappingClientImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicMappingClientTest extends AbstractWireMockRestClientTest {

    public static final String RESOURCE_PATH_DELIMETER = "/";

    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMETER + "data" + RESOURCE_PATH_DELIMETER + "ieee14";
    public static final String INPUT = "input";
    public static final String MODELS_GROOVY = "models.groovy";
    public static final String MODELS_PAR = "models.par";

    private static final String FIXED_DATE = "01/01/2023";
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

    private DynamicMappingClient dynamicMappingClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void setup() {
        super.setup();
        dynamicMappingClient = new DynamicMappingClientImpl(
                // use new WireMockServer(DYNAMIC_MAPPING_PORT) to test with local server if needed
                initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
                restTemplate);
    }

    @Test
    public void testCreateFromMapping() throws IOException, ParseException {
        String mappingName = MAPPING_NAME_01;

        // prepare script
        String scriptJson;
        // load models.groovy
        String inputDir = DATA_IEEE14_BASE_DIR +
                          RESOURCE_PATH_DELIMETER + mappingName +
                          RESOURCE_PATH_DELIMETER + INPUT;
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
                mappingName + "-script",
                mappingName,
                script,
                dateFormat.parse(FIXED_DATE),
                parametersFile);

        ObjectWriter ow = objectMapper.writer().withDefaultPrettyPrinter();
        scriptJson = ow.writeValueAsString(scriptObj);

        // mock response for test case GET with url - /scripts/from/{MAPPING_NAME_01}
        String baseUrl = DELIMITER + API_VERSION + DELIMITER +
                         DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT + DELIMITER;
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathTemplate(baseUrl + DELIMITER + "{mappingName}"))
                .withPathParam("mappingName", equalTo(mappingName))
                .willReturn(WireMock.ok()
                        .withBody(scriptJson)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                ));

        Script createdScript = dynamicMappingClient.createFromMapping(MAPPING_NAME_01);

        // check result
        // models.groovy
        assertEquals(script, Optional.of(createdScript).orElseThrow().getScript());
        // load models.par
        assertEquals(parametersFile, createdScript.getParametersFile());
    }
}
