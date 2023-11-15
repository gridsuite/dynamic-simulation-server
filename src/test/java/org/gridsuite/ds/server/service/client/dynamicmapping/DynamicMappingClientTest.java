/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client.dynamicmapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.client.AbstractRestClientTest;
import org.gridsuite.ds.server.service.client.dynamicmapping.impl.DynamicMappingClientImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicMappingClientTest extends AbstractRestClientTest {

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

    @Override
    @NotNull
    protected Dispatcher getDispatcher() {
        return new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
                String path = Objects.requireNonNull(recordedRequest.getPath());
                String baseUrl = DELIMITER + API_VERSION + DELIMITER +
                        DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT + DELIMITER;
                baseUrl = baseUrl.replace("//", "/");
                String method = recordedRequest.getMethod();
                MockResponse response = new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
                // scripts/from/{mappingName}
                if ("GET".equals(method)
                        && path.matches(baseUrl + ".*")) {
                    // take {mappingName} at the last
                    String mappingName = emptyIfNull(recordedRequest.getRequestUrl().pathSegments()).stream().reduce((first, second) -> second).orElse("");
                    if (MAPPING_NAME_01.equals(mappingName)) {
                        String scriptJson;
                        try {
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

                            ObjectWriter ow = getObjectMapper().writer().withDefaultPrettyPrinter();
                            scriptJson = ow.writeValueAsString(scriptObj);
                        } catch (JsonProcessingException e) {
                            logger.info("Cannot convert to json : ", e);
                            return new MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value());
                        } catch (IOException e) {
                            logger.info("Cannot read file : ", e);
                            return new MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value());
                        } catch (ParseException e) {
                            logger.info("Cannot parse date: ", e);
                            return new MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value());
                        }

                        response = new MockResponse()
                                .setResponseCode(HttpStatus.OK.value())
                                .addHeader("Content-Type", "application/json; charset=utf-8")
                                .setBody(scriptJson);
                    }
                }
                return response;
            }
        };
    }

//    /**
//     * used for test with local server
//     */
//    @Override
//    protected String initMockWebServer(int port) throws RuntimeException {
//        return "http://localhost:" + DYNAMIC_MAPPING_PORT;
//    }

    @Override
    public void setUp() {
        super.setUp();

        // config builder
        WebClient.Builder webClientBuilder = WebClient.builder();
        dynamicMappingClient = new DynamicMappingClientImpl(webClientBuilder, initMockWebServer());
    }

    @Test
    public void testCreateFromMapping() throws IOException {
        String mappingName = MAPPING_NAME_01;
        Script createdScript = dynamicMappingClient.createFromMapping(MAPPING_NAME_01).block();

        // load models.groovy
        String inputDir = DATA_IEEE14_BASE_DIR +
                RESOURCE_PATH_DELIMETER + mappingName +
                RESOURCE_PATH_DELIMETER + INPUT;
        String scriptPath = inputDir + RESOURCE_PATH_DELIMETER + MODELS_GROOVY;
        InputStream scriptIS = getClass().getResourceAsStream(scriptPath);
        byte[] scriptBytes;
        scriptBytes = StreamUtils.copyToByteArray(scriptIS);
        String script = new String(scriptBytes, StandardCharsets.UTF_8);

        assertEquals(script, Optional.of(createdScript).orElseThrow().getScript());

        // load models.par
        String parametersFilePath = inputDir + RESOURCE_PATH_DELIMETER + MODELS_PAR;
        InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
        byte[] parametersFileBytes;
        parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
        String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

        assertEquals(parametersFile, createdScript.getParametersFile());
    }
}
