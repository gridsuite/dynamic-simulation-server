/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.dynamicmapping.impl;

import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.client.AbstractRestClient;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.CREATE_MAPPING_SCRIPT_ERROR;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.DYNAMIC_MAPPING_NOT_FOUND;
import static org.gridsuite.ds.server.service.client.utils.UrlUtils.buildEndPointUrl;
import static org.gridsuite.ds.server.utils.ExceptionUtils.handleHttpError;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicMappingClientImpl extends AbstractRestClient implements DynamicMappingClient {

    @Autowired
    public DynamicMappingClientImpl(@Value("${gridsuite.services.dynamic-mapping-server.base-uri:http://dynamic-mapping-server/}") String baseUri, RestTemplate restTemplate) {
        super(baseUri, restTemplate);
    }

    @Override
    public Script createFromMapping(String mappingName) {
        Objects.requireNonNull(mappingName);

        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl + "{mappingName}");

        // to export script and not persist
        uriComponentsBuilder.queryParam("persistent", false);
        var uriComponents = uriComponentsBuilder.buildAndExpand(mappingName);

        // call dynamic mapping Rest API
        try {
            return getRestTemplate().getForObject(uriComponents.toUriString(), Script.class);
        } catch (HttpStatusCodeException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                throw new DynamicSimulationException(DYNAMIC_MAPPING_NOT_FOUND, "Mapping not found: " + mappingName);
            } else {
                throw handleHttpError(e, CREATE_MAPPING_SCRIPT_ERROR);
            }
        }
    }

}
