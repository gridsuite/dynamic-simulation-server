/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.timeseries.impl;

import com.powsybl.timeseries.TimeSeries;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class TimeSeriesClientImpl implements TimeSeriesClient {

    private final WebClient webClient;

    public TimeSeriesClientImpl(WebClient.Builder builder, @Value("${gridsuite.services.timeseries-server.base-uri:http://timeseries-server/}") String baseUri) {
        webClient = builder.baseUrl(baseUri).build();
    }

    @Override
    public Mono<Map<String, UUID>> sendTimeSeries(List<TimeSeries> timeSeriesList) throws HttpClientErrorException {
        String url = API_VERSION + DELIMITER + TIME_SERIES_END_POINT;

        // convert timeseries to json
        var timeSeriesListJson = TimeSeries.toJson(timeSeriesList);

        // call time-series Rest API
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .build())
                .body(BodyInserters.fromValue(timeSeriesListJson))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, UUID>>() { });
    }
}
