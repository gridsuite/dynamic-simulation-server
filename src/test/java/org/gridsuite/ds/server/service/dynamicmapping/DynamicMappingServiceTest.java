/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.dynamicmapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.AbstractServiceTest;
import org.gridsuite.ds.server.service.dynamicmapping.implementation.DynamicMappingServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.gridsuite.ds.server.service.dynamicmapping.DynamicMappingService.API_VERSION;
import static org.gridsuite.ds.server.service.parameters.ParametersService.MODELS_PAR;
import static org.gridsuite.ds.server.service.timeseries.TimeSeriesService.DELIMITER;
import static org.junit.Assert.assertEquals;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicMappingServiceTest extends AbstractServiceTest {
    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = "/data/ieee14";
    public static final String INPUT = "input";
    public static final String MODELS_GROOVY = "models.groovy";
    private static final int DYNAMIC_MAPPING_PORT = 5036;

    private DynamicMappingService dynamicMappingService;

    @Override
    @NotNull
    protected Dispatcher getDispatcher() {
        return new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
                String path = Objects.requireNonNull(recordedRequest.getPath());
                String baseUrl = DELIMITER + API_VERSION + DELIMITER + DynamicMappingService.DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT + DELIMITER;
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
                            String scriptPath = Paths.get(DATA_IEEE14_BASE_DIR, mappingName, INPUT, MODELS_GROOVY).toString();
                            InputStream scriptIS = getClass().getResourceAsStream(scriptPath);
                            byte[] scriptBytes;
                            scriptBytes = StreamUtils.copyToByteArray(scriptIS);
                            String script = new String(scriptBytes, StandardCharsets.UTF_8);

                            // load models.par
                            String parametersFilePath = Paths.get(DATA_IEEE14_BASE_DIR, mappingName, INPUT, MODELS_PAR).toString();
                            InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
                            byte[] parametersFileBytes;
                            parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
                            String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

                            Script scriptObj =  new Script(
                                    mappingName + "-script",
                                    mappingName,
                                    script,
                                    new Date(),
                                    true,
                                    parametersFile);

                            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                            scriptJson = ow.writeValueAsString(scriptObj);
                        } catch (JsonProcessingException e) {
                            getLogger().info("Cannot convert to json : ", e);
                            return new MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value());
                        } catch (IOException e) {
                            getLogger().info("Cannot read file : ", e);
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
    public void setUp() throws IOException {
        super.setUp();

        // config builder
        WebClient.Builder webClientBuilder = WebClient.builder();
        dynamicMappingService = new DynamicMappingServiceImpl(webClientBuilder, initMockWebServer(DYNAMIC_MAPPING_PORT));
    }

    @Test
    public void testCreateFromMapping() throws IOException {
        String mappingName = MAPPING_NAME_01;
        Script createdScript = dynamicMappingService.createFromMapping(MAPPING_NAME_01).block();

        // load models.groovy
        String scriptPath = Paths.get(DATA_IEEE14_BASE_DIR, mappingName, INPUT, MODELS_GROOVY).toString();
        InputStream scriptIS = getClass().getResourceAsStream(scriptPath);
        byte[] scriptBytes;
        scriptBytes = StreamUtils.copyToByteArray(scriptIS);
        String script = new String(scriptBytes, StandardCharsets.UTF_8);

        assertEquals(script, Optional.of(createdScript).orElseThrow().getScript());

        // load models.par
        String parametersFilePath = Paths.get(DATA_IEEE14_BASE_DIR, mappingName, INPUT, MODELS_PAR).toString();
        InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
        byte[] parametersFileBytes;
        parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
        String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

        assertEquals(parametersFile, createdScript.getParametersFile());
    }
}
