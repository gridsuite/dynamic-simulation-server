package org.gridsuite.ds.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.dynamicmapping.DynamicMappingService;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;

import static org.gridsuite.ds.server.service.timeseries.TimeSeriesService.*;

@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient(timeout = "PT360S")
@EnableWebFlux
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
        initializers = CustomApplicationContextInitializer.class)
public abstract class AbstractDynamicSimulationTest {
    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractDynamicSimulationTest.class);

    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = "/data/ieee14";
    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final String TEST_ENV_BASE_DIR = "/work/unittests";
    public static final String MODELS_PAR = "models.par";
    public static final String NETWORK_PAR = "network.par";
    public static final String SOLVERS_PAR = "solvers.par";
    public static final String MODELS_GROOVY = "models.groovy";
    public static final String EVENTS_GROOVY = "events.groovy";
    public static final String CURVES_GROOVY = "curves.groovy";
    public static final String PARAMETERS_JSON = "parameters.json";
    public static final String RESULT_JSON = "result.json";

    // time-series-server mocks
    private static final int TIME_SERIES_PORT = 5034;
    public static final String TIME_SERIES_UUID = "33333333-0000-0000-0000-000000000000";
    public static final String TIME_LINE_UUID = "44444444-0000-0000-0000-000000000000";
    static MockWebServer timeSeriesServer;

    // dynamic-mapping-server mocks
    private static final int DYNAMIC_MAPPING_PORT = 5036;
    static MockWebServer dynamicMappingServer;

    static {
        // --- SETUP mock time-series-server --- //
        timeSeriesServer = new MockWebServer();
        try {
            timeSeriesServer.start(TIME_SERIES_PORT);
        } catch (IOException e) {
            throw new RuntimeException("Can not init the mock time-series-server", e);
        }

        // setup dispatcher
        var timeSeriesDispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                String path = Objects.requireNonNull(recordedRequest.getPath());
                String baseUrl = DELIMITER + TIME_SERIES_END_POINT + DELIMITER;
                String method = recordedRequest.getMethod();

                // timeseries/{groupUuid}
                if ("POST".equals(method)
                        && path.matches(baseUrl + TIME_SERIES_GROUP_UUID + ".*")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .setBody(TIME_SERIES_UUID);
                } else if ("POST".equals(method)
                        && path.matches(baseUrl + TIME_LINE_GROUP_UUID + ".*")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .setBody(TIME_LINE_UUID);
                }
                return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
            }
        };
        // attach dispatcher
        timeSeriesServer.setDispatcher(timeSeriesDispatcher);

        // --- SETUP mock dynamic-simulation-server --- //
        dynamicMappingServer = new MockWebServer();
        try {
            dynamicMappingServer.start(DYNAMIC_MAPPING_PORT);
        } catch (IOException e) {
            throw new RuntimeException("Can not init the mock dynamic-mapping-server", e);
        }

        // setup dispatcher
        var dynamicMappingDispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                String path = Objects.requireNonNull(recordedRequest.getPath());
                String baseScriptCreateUrl = DELIMITER + DynamicMappingService.DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT + DELIMITER;
                String method = recordedRequest.getMethod();

                // scripts/from/{mappingName}
                if ("GET".equals(method)
                        && path.matches(baseScriptCreateUrl + ".*")) {
                    String mappingName = recordedRequest.getRequestUrl().pathSegments().get(2);
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
                            LOGGER.info("Cannot convert to json : ", e);
                            return new MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value());
                        } catch (IOException e) {
                            LOGGER.info("Cannot read file : ", e);
                            return new MockResponse().setResponseCode(HttpStatus.NO_CONTENT.value());
                        }

                        return new MockResponse()
                                .setResponseCode(HttpStatus.OK.value())
                                .addHeader("Content-Type", "application/json; charset=utf-8")
                                .setBody(scriptJson);
                    }
                }
                return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
            }
        };
        // attach dispatcher
        dynamicMappingServer.setDispatcher(dynamicMappingDispatcher);
    }
}
