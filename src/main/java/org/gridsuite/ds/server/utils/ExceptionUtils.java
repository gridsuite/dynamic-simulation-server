/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * The implementation of class {@link ExceptionUtils} is taken from class {@code StudyUtils} in study-server
 *
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ExceptionUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionUtils.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ExceptionUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static DynamicSimulationException handleHttpError(HttpStatusCodeException httpException, DynamicSimulationException.Type type) {
        String responseBody = httpException.getResponseBodyAsString();

        String errorMessage = responseBody.isEmpty() ? httpException.getStatusCode().toString() : parseHttpError(responseBody);

        LOGGER.error(errorMessage, httpException);

        return new DynamicSimulationException(type, errorMessage);
    }

    private static String parseHttpError(String responseBody) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(responseBody).path("message");
            if (!node.isMissingNode()) {
                return node.asText();
            }
        } catch (JsonProcessingException e) {
            // status code or responseBody by default
        }

        return responseBody;
    }
}
