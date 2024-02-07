/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client.timeseries;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.timeseries.TimeSeriesIndex;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.AbstractWireMockRestClientTest;
import org.gridsuite.ds.server.service.client.timeseries.impl.TimeSeriesClientImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.CREATE_TIME_SERIES_ERROR;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.DELETE_TIME_SERIES_ERROR;
import static org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient.API_VERSION;
import static org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient.TIME_SERIES_END_POINT;
import static org.gridsuite.ds.server.service.client.utils.UrlUtils.buildEndPointUrl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class TimeSeriesClientTest extends AbstractWireMockRestClientTest {

    public static final UUID TIME_SERIES_UUID = UUID.randomUUID();
    public static final UUID TIME_LINE_UUID = UUID.randomUUID();
    private TimeSeriesClient timeSeriesClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static List<TimeSeries> createTimeSeriesList() {
        Map<String, TimeSeries> curves = new HashMap<>();
        TimeSeriesIndex index = new IrregularTimeSeriesIndex(new long[]{32, 64, 128, 256});
        curves.put("NETWORK__BUS____2-BUS____5-1_AC_iSide2", TimeSeries.createDouble("NETWORK__BUS____2-BUS____5-1_AC_iSide2", index, 333.847331, 333.847321, 333.847300, 333.847259));
        curves.put("NETWORK__BUS____1_TN_Upu_value", TimeSeries.createDouble("NETWORK__BUS____1_TN_Upu_value", index, 1.059970, 1.059970, 1.059970, 1.059970));
        List<TimeSeries> timeSeries = new ArrayList<>(curves.values());
        return timeSeries;
    }

    private static String getEndpointUrl() {
        return buildEndPointUrl("", API_VERSION,
                TIME_SERIES_END_POINT);
    }

    @Override
    public void setup() {
        super.setup();
        timeSeriesClient = new TimeSeriesClientImpl(
            // use new WireMockServer(TIME_SERIES_PORT) to test with local server if needed
            initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
            restTemplate);
    }

    @Test
    public void testSendTimeSeries() throws JsonProcessingException {

        // prepare time series
        List<TimeSeries> timeSeries = createTimeSeriesList();

        // mock response for test case POST with url - /timeseries-group
        String baseUrl = getEndpointUrl();

        wireMockServer.stubFor(WireMock.post(WireMock.urlMatching(baseUrl + ".*"))
                .willReturn(WireMock.ok()
                        .withBody(objectMapper.writeValueAsString(new TimeSeriesGroupInfos(TIME_SERIES_UUID)))
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                ));

        // test service
        UUID timeSeriesUuid = timeSeriesClient.sendTimeSeries(timeSeries).getId();

        // check result
        assertEquals(TIME_SERIES_UUID, timeSeriesUuid);

    }

    @Test
    public void testSendTimeSeriesGivenException() {
        // prepare time series
        List<TimeSeries> timeSeries = createTimeSeriesList();

        // mock response for test case POST with url - /timeseries-group
        String baseUrl = getEndpointUrl();

        wireMockServer.stubFor(WireMock.post(WireMock.urlMatching(baseUrl + ".*"))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE_JSON)));

        // test service
        DynamicSimulationException dynamicSimulationException = catchThrowableOfType(
                () -> timeSeriesClient.sendTimeSeries(timeSeries),
                DynamicSimulationException.class);

        // check result
        assertThat(dynamicSimulationException.getType())
                .isEqualTo(CREATE_TIME_SERIES_ERROR);
        assertThat(dynamicSimulationException.getMessage())
                .isEqualTo(ERROR_MESSAGE);

    }

    @Test
    public void testSendTimeSeriesGivenEmpty() throws JsonProcessingException {
        // prepare time series
        List<TimeSeries> timeSeries = new ArrayList<>();

        // test service
        TimeSeriesGroupInfos timeSeriesGroupInfos = timeSeriesClient.sendTimeSeries(timeSeries);

        // check result
        assertEquals(null, timeSeriesGroupInfos);

    }

    @Test
    public void testDeleteTimeSeriesGroup() {

        // mock response for test case DELETE with url - /timeseries-group
        String baseUrl = getEndpointUrl();
        wireMockServer.stubFor(WireMock.delete(WireMock.urlMatching(baseUrl + ".*"))
                .willReturn(WireMock.ok()));

        // test service
        timeSeriesClient.deleteTimeSeriesGroup(TIME_LINE_UUID);

        // check result
        assertTrue(true);
    }

    @Test
    public void testDeleteTimeSeriesGroupGivenException() {

        // mock response for test case DELETE with url - /timeseries-group
        String baseUrl = getEndpointUrl();
        wireMockServer.stubFor(WireMock.delete(WireMock.urlMatching(baseUrl + ".*"))
                .willReturn(WireMock.serverError()
                        .withBody(ERROR_MESSAGE)));

        // test service
        DynamicSimulationException dynamicSimulationException = catchThrowableOfType(
                () -> timeSeriesClient.deleteTimeSeriesGroup(TIME_LINE_UUID),
                DynamicSimulationException.class);

        // check result
        assertThat(dynamicSimulationException.getType())
                .isEqualTo(DELETE_TIME_SERIES_ERROR);
        assertThat(dynamicSimulationException.getMessage())
                .isEqualTo(ERROR_MESSAGE);
    }
}
