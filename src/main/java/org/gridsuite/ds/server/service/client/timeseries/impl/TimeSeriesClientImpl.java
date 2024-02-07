/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.timeseries.impl;

import com.powsybl.timeseries.TimeSeries;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.AbstractRestClient;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.CREATE_TIME_SERIES_ERROR;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.DELETE_TIME_SERIES_ERROR;
import static org.gridsuite.ds.server.service.client.utils.UrlUtils.buildEndPointUrl;
import static org.gridsuite.ds.server.utils.ExceptionUtils.handleHttpError;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class TimeSeriesClientImpl extends AbstractRestClient implements TimeSeriesClient {

    @Autowired
    public TimeSeriesClientImpl(@Value("${gridsuite.services.timeseries-server.base-uri:http://timeseries-server/}") String baseUri,
                                RestTemplate restTemplate) {
        super(baseUri, restTemplate);
    }

    @Override
    public TimeSeriesGroupInfos sendTimeSeries(List<TimeSeries<?, ?>> timeSeriesList) {
        if (CollectionUtils.isEmpty(timeSeriesList)) {
            return null;
        }

        // convert timeseries to json
        var timeSeriesListJson = TimeSeries.toJson(timeSeriesList);

        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, TIME_SERIES_END_POINT);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl);
        var uriComponents = uriComponentsBuilder.build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // call time-series Rest API
        HttpEntity<String> httpEntity = new HttpEntity<>(timeSeriesListJson, headers);

        try {
            return getRestTemplate().postForObject(uriComponents.toUriString(), httpEntity, TimeSeriesGroupInfos.class);
        } catch (HttpStatusCodeException e) {
            throw handleHttpError(e, CREATE_TIME_SERIES_ERROR);
        }
    }

    @Override
    public void deleteTimeSeriesGroup(UUID groupUuid) {
        if (groupUuid == null) {
            return;
        }

        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, TIME_SERIES_END_POINT);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl + DELIMITER + "{groupUuid}");
        var uriComponents = uriComponentsBuilder.buildAndExpand(groupUuid);

        // call time-series Rest API
        try {
            getRestTemplate().delete(uriComponents.toUriString());
        } catch (HttpStatusCodeException e) {
            throw handleHttpError(e, DELETE_TIME_SERIES_ERROR);
        }
    }
}
