package org.gridsuite.ds.server.service.dynamicmapping;

import org.gridsuite.ds.server.dto.dynamicmapping.Script;

public interface DynamicMappingService {
    String DELIMITER = "/";
    String DYNAMIC_MAPPING_SCRIPT_BASE_END_POINT = "scripts";
    String DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT = DYNAMIC_MAPPING_SCRIPT_BASE_END_POINT + DELIMITER + "from";

    Script createFromMapping(String mappingName);
}
