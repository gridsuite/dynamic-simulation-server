/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.gridsuite.ds.server.dto.dynawaltz.DynaWaltzParametersInfos;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "name",
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true
)@JsonSubTypes({
    @JsonSubTypes.Type(value = DynaWaltzParametersInfos.class, name = "DynaWaltzParameters")})
public interface DynamicSimulationExtension {
    String getName();
}
