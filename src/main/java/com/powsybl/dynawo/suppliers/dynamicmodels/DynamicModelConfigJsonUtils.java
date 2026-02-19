/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.dynawo.suppliers.dynamicmodels;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.commons.json.JsonUtil;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class DynamicModelConfigJsonUtils {

    private DynamicModelConfigJsonUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = JsonUtil.createObjectMapper();

        SimpleModule module = new SimpleModule("dynamic-model-configs");
        module.addSerializer(new DynamicModelConfigsJsonSerializer());
        module.addDeserializer(List.class, new DynamicModelConfigsJsonDeserializer());

        mapper.registerModule(module);
        return mapper;
    }
}
