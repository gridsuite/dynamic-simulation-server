/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.dynawo.suppliers.dynamicmodels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.gridsuite.ds.server.controller.utils.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
class DynamicModelConfigsJsonSerializerTest {

    @Test
    void testModelConfigDeserializerSerializer() throws IOException {

        ObjectMapper objectMapper = DynamicModelConfigJsonUtils.createObjectMapper();
        List<DynamicModelConfig> dynamicModelConfigs = objectMapper.readValue(getClass().getResourceAsStream("/data/dynamicModels.json"), new TypeReference<>() { });
        assertEquals(2, dynamicModelConfigs.size());

        String dynamicModelConfigsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dynamicModelConfigs);
        List<DynamicModelConfig> dynamicModelConfigs2 = objectMapper.readValue(dynamicModelConfigsJson, new TypeReference<>() { });
        FileUtils.writeBytesToFile(this, "/data/dynamicModels_exported.json", dynamicModelConfigsJson.getBytes());

        Assertions.assertThat(dynamicModelConfigs2).usingRecursiveComparison().isEqualTo(dynamicModelConfigs);
    }
}
