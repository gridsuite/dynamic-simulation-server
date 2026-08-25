/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface RestClientI {
    String URL_DELIMITER = "/";

    RestClient getRestClient();

    ObjectMapper getObjectMapper();

    String getBaseUri();
}
