/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.dynawaltz.solver;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.gridsuite.ds.server.dto.dynawaltz.XmlSerializableParameter;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true
)@JsonSubTypes({
    @JsonSubTypes.Type(value = IdaSolverInfos.class, name = "IDA"),
    @JsonSubTypes.Type(value = SimSolverInfos.class, name = "SIM")})
public interface SolverInfos extends XmlSerializableParameter {
    String getId();

    SolverTypeInfos getType();

}
