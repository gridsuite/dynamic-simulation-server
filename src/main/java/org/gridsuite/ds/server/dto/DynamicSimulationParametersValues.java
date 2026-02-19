/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfigsJsonDeserializer;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfigsJsonSerializer;
import lombok.*;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicSimulationParametersValues {
    @JsonSerialize(using = DynamicModelConfigsJsonSerializer.class)
    @JsonDeserialize(using = DynamicModelConfigsJsonDeserializer.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    List<DynamicModelConfig> dynamicModel;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    DynawoSimulationParameters dynawoParameters;
}
