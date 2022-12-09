/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.timeseries.implementation;

import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.TimeSeries;
import org.gridsuite.ds.server.service.timeseries.TimeSeriesService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class TimeSeriesServiceImpl implements TimeSeriesService {

    private final String baseUri;

    private RestTemplate restTemplate;

    public TimeSeriesServiceImpl(@Value("${time-series-server.base-uri:http://time-series-server/}") String baseUri) {
        this.baseUri = baseUri;

    }

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplateBuilder().build();
    }

    @Override public UUID sendTimeSeries(List<TimeSeries> timeSeriesList) throws HttpClientErrorException {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = baseUri + DELIMITER + TIME_SERIES_END_POINT;
        var uriBuilder = UriComponentsBuilder.fromHttpUrl(url);

        // convert timeseries to json
        var timeSeriesListJson = TimeSeries.toJson(timeSeriesList);

        // call time-series Rest API
        var responseEntity = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, new HttpEntity<>(timeSeriesListJson, headers), String.class);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return UUID.fromString(responseEntity.getBody());
        } else {
            throw new PowsyblException("Can not send time series to server: HttpStatus = " + responseEntity.getStatusCode());
        }
    }
}
