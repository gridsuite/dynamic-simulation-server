/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.dynamicmapping.impl;

import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class DynamicMappingClientImpl implements DynamicMappingClient {

    private final WebClient webClient;

    public DynamicMappingClientImpl(WebClient.Builder builder, @Value("${gridsuite.services.dynamic-mapping-server.base-uri:http://dynamic-mapping-server/}") String baseUri) {
        webClient = builder.baseUrl(baseUri).build();
    }

    @Override
    public Mono<Script> createFromMapping(String mappingName) {
        String url = API_VERSION + DELIMITER + DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT + DELIMITER + "{mappingName}";

        // call dynamic mapping Rest API
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(url)
                        .queryParam("persistent", false)
                        .build(mappingName))
                .retrieve()
                .bodyToMono(Script.class);

    }
}
