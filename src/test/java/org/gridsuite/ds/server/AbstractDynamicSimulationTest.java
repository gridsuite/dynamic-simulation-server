package org.gridsuite.ds.server;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.ds.server.service.timeseries.TimeSeriesService;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.config.EnableWebFlux;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient(timeout = "PT360S")
@EnableWebFlux
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
        initializers = CustomApplicationContextInitializer.class)
public abstract class AbstractDynamicSimulationTest {
    private static final int TIME_SERIES_PORT = 5034;
    static MockWebServer timeSeriesServer;

    // setup mock time-series-server
    static {
        timeSeriesServer = new MockWebServer();
        try {
            timeSeriesServer.start(TIME_SERIES_PORT);
        } catch (IOException e) {
            throw new RuntimeException("Can not init the mock time-series-server", e);
        }

        // setup dispatcher
        var dispatcher = new Dispatcher() {
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) throws InterruptedException {
                String path = Objects.requireNonNull(recordedRequest.getPath());
                if ("POST".equals(recordedRequest.getMethod())
                        && path.matches(TimeSeriesService.DELIMITER + TimeSeriesService.TIME_SERIES_END_POINT + ".*")
                ) {
                    UUID uuid = UUID.randomUUID();
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .setBody(uuid.toString());
                }
                return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
            }
        };
        // attach dispatcher
        timeSeriesServer.setDispatcher(dispatcher);
    }
}
