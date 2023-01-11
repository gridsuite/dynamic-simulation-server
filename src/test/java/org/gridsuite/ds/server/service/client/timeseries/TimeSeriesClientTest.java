/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client.timeseries;

import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import lombok.SneakyThrows;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.gridsuite.ds.server.service.client.AbstractRestClientTest;
import org.gridsuite.ds.server.service.client.timeseries.impl.TimeSeriesClientImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.*;

import static org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class TimeSeriesClientTest extends AbstractRestClientTest {
    private static final int TIME_SERIES_PORT = 5037;
    public static final String TIME_SERIES_UUID = "33333333-0000-0000-0000-000000000000";

    private TimeSeriesClient timeSeriesClient;

    @Override
    @NotNull
    protected Dispatcher getDispatcher() {
        return new Dispatcher() {
            @SneakyThrows
            @NotNull
            @Override
            public MockResponse dispatch(@NotNull RecordedRequest recordedRequest) {
                String path = Objects.requireNonNull(recordedRequest.getPath());
                String baseUrl = DELIMITER + API_VERSION + DELIMITER + TIME_SERIES_END_POINT;
                baseUrl = baseUrl.replace("//", "/");
                String method = recordedRequest.getMethod();

                // v1/time-series
                if ("POST".equals(method)
                        && path.matches(baseUrl + ".*")) {
                    return new MockResponse()
                            .setResponseCode(HttpStatus.OK.value())
                            .addHeader("Content-Type", "application/json; charset=utf-8")
                            .setBody(objectMapper.writeValueAsString(Map.of(UUID_KEY, UUID.fromString(TIME_SERIES_UUID))));
                }
                return new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
            }
        };
    }

//    /**
//     * Used for test with a local server
//     * @throws RuntimeException
//     */
//    @Override
//    protected String initMockWebServer(int port) throws RuntimeException {
//        return "http://localhost:" + TIME_SERIES_PORT;
//    }

    @Before
    public void setUp() throws IOException {
        super.setUp();

        // config builder
        WebClient.Builder webClientBuilder = WebClient.builder();
        timeSeriesClient = new TimeSeriesClientImpl(webClientBuilder, initMockWebServer(TIME_SERIES_PORT));
    }

    @Test
    public void testSendTimeSeries() {
        Map<String, TimeSeries> curves = new HashMap<>();
        TimeSeriesIndex index = new IrregularTimeSeriesIndex(new long[]{32, 64, 128, 256});
        curves.put("NETWORK__BUS____2-BUS____5-1_AC_iSide2", TimeSeries.createDouble("NETWORK__BUS____2-BUS____5-1_AC_iSide2", index, 333.847331, 333.847321, 333.847300, 333.847259));
        curves.put("NETWORK__BUS____1_TN_Upu_value", TimeSeries.createDouble("NETWORK__BUS____1_TN_Upu_value", index, 1.059970, 1.059970, 1.059970, 1.059970));
        List<TimeSeries> timeSeries = new ArrayList<>(curves.values());
        UUID timeSeriesUuid = timeSeriesClient.sendTimeSeries(timeSeries).block().getOrDefault(UUID_KEY, null);
        assertEquals(TIME_SERIES_UUID, Optional.of(timeSeriesUuid).orElseThrow().toString());

    }
}
