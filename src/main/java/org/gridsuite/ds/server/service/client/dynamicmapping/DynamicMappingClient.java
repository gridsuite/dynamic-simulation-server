/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.client.dynamicmapping;

import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.Parameter;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface DynamicMappingClient {
    String API_VERSION = "";
    String DYNAMIC_MAPPING_PARAMETER_BASE_ENDPOINT = "parameters";
    String DYNAMIC_MAPPING_MAPPING_BASE_ENDPOINT = "mappings";
    String DYNAMIC_MAPPING_PARAMETER_GET_ENDPOINT = DYNAMIC_MAPPING_PARAMETER_BASE_ENDPOINT;

    Parameter getParameters(String mappingName);

    InputMapping getMapping(String mappingName);
}
