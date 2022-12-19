/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.dynamicmapping;

import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import reactor.core.publisher.Mono;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface DynamicMappingService {
    String API_VERSION = "";
    String DELIMITER = "/";
    String DYNAMIC_MAPPING_SCRIPT_BASE_END_POINT = "scripts";
    String DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT = DYNAMIC_MAPPING_SCRIPT_BASE_END_POINT + DELIMITER + "from";

    Mono<Script> createFromMapping(String mappingName);
}
