/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.dynamicmapping.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.ParameterFile;
import org.gridsuite.ds.server.service.client.AbstractRestClient;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import static org.gridsuite.ds.server.service.client.utils.UrlUtils.buildEndPointUrl;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicMappingClientImpl extends AbstractRestClient implements DynamicMappingClient {

    @Autowired
    public DynamicMappingClientImpl(@Value("${gridsuite.services.dynamic-mapping-server.base-uri:http://dynamic-mapping-server/}") String baseUri, RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(baseUri, restTemplate, objectMapper);
    }

    @Override
    public ParameterFile exportParameters(@NonNull String mappingName) {
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_MAPPING_PARAMETERS_EXPORT_ENDPOINT);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl);
        uriComponentsBuilder.queryParam("mappingName", mappingName);
        var uriComponents = uriComponentsBuilder.build();

        // call dynamic mapping Rest API
        return getRestTemplate().getForObject(uriComponents.toUriString(), ParameterFile.class);
    }

    @Override
    public InputMapping getMapping(@NonNull String mappingName) {
        String endPointUrl = buildEndPointUrl(getBaseUri(), API_VERSION, DYNAMIC_MAPPING_MAPPINGS_BASE_ENDPOINT);

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl + URL_DELIMITER + "{mappingName}");

        UriComponents uriComponents = uriComponentsBuilder.buildAndExpand(mappingName);

        // call dynamic mapping Rest API
        return getRestTemplate().getForObject(uriComponents.toUriString(), InputMapping.class);
    }

}
