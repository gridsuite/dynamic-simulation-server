/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public abstract class AbstractRestClient implements RestClientI {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final RestClient restClient;

    private final String baseUri;

    private final ObjectMapper objectMapper;

    protected AbstractRestClient(String baseUri, RestClient restClient, ObjectMapper objectMapper) {
        this.baseUri = baseUri;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RestClient getRestClient() {
        return restClient;
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public String getBaseUri() {
        return baseUri;
    }
}
