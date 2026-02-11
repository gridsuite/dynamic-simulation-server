/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.filter.AbstractFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.gridsuite.ds.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class FilterClient extends AbstractRestClient {

    public static final String API_VERSION = "v1";
    public static final String FILTERS_BASE_ENDPOINT = "filters";
    public static final String FILTERS_GET_ENDPOINT = FILTERS_BASE_ENDPOINT + URL_DELIMITER + "metadata";

    protected FilterClient(
            @Value("${gridsuite.services.filter-server.base-uri:http://filter-server/}") String baseUri,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(baseUri, restTemplate, objectMapper);
    }

    public List<AbstractFilter> getFilters(List<UUID> filterUuids) {
        if (CollectionUtils.isEmpty(filterUuids)) {
            return Collections.emptyList();
        }

        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, FILTERS_GET_ENDPOINT);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(endPointUrl);
        uriComponentsBuilder.queryParam("ids", filterUuids);

        // call filter server Rest API
        return getRestTemplate().exchange(
                uriComponentsBuilder.build().toUriString(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AbstractFilter>>() {
                }).getBody();
    }
}
