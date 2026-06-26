/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.powsybl.dynawo.DynawoSimulationParameters.SolverType;
import lombok.*;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.network.NetworkInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;

import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicSimulationParametersInfos {
    private UUID id;
    private String provider;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double startTime;
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Double stopTime;
    private UUID mappingId;
    private SolverType solver;
    private List<SolverInfos> solvers;
    private NetworkInfos network;
    private List<CurveInfos> curves;
}
