/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.dynamicmapping;

import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.Parameter;

import static org.gridsuite.ds.server.service.client.RestClient.DELIMITER;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface DynamicMappingClient {
    String API_VERSION = "";
    String DYNAMIC_MAPPING_PARAMETER_BASE_END_POINT = "parameters";
    String DYNAMIC_MAPPING_MAPPING_BASE_END_POINT = "mappings";
    String DYNAMIC_MAPPING_PARAMETER_CREATE_END_POINT = DYNAMIC_MAPPING_PARAMETER_BASE_END_POINT + DELIMITER + "from";

    Parameter createFromMapping(String mappingName);

    InputMapping getMapping(String mappingName);
}
