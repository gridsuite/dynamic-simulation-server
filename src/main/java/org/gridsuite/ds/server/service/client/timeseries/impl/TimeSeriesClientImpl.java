/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.timeseries.impl;

import com.powsybl.timeseries.TimeSeries;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class TimeSeriesClientImpl implements TimeSeriesClient {

    public static final String BASE_END_POINT_URI = API_VERSION + DELIMITER + TIME_SERIES_END_POINT;
    private final WebClient webClient;

    public TimeSeriesClientImpl(WebClient.Builder builder, @Value("${gridsuite.services.timeseries-server.base-uri:http://timeseries-server/}") String baseUri) {
        webClient = builder.baseUrl(baseUri).build();
    }

    @Override
    public Mono<TimeSeriesGroupInfos> sendTimeSeries(List<TimeSeries> timeSeriesList) {

        // convert timeseries to json
        var timeSeriesListJson = TimeSeries.toJson(timeSeriesList);

        // call time-series Rest API
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_END_POINT_URI)
                        .build())
                .body(BodyInserters.fromValue(timeSeriesListJson))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<TimeSeriesGroupInfos>() { });
    }

    @Override
    public Mono<Void> deleteTimeSeriesGroup(UUID groupUuid) {
        if (groupUuid == null) {
            return Mono.empty();
        }

        // call time-series Rest API
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path(BASE_END_POINT_URI + DELIMITER + "{groupUuid}")
                        .build(groupUuid))
                .retrieve()
                .bodyToMono(Void.class);
    }
}
