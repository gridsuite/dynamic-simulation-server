/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.dynawaltz;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.gridsuite.ds.server.dto.DynamicSimulationExtension;
import org.gridsuite.ds.server.dto.dynawaltz.solver.SolverInfos;

import java.util.List;

@SuperBuilder
@AllArgsConstructor
@Getter
@Setter
public class DynaWaltzParametersInfos implements DynamicSimulationExtension {
    public static final String EXTENSION_NAME = "DynaWaltzParameters";
    private final String name;
    private String solverId;
    private List<SolverInfos> solvers;

    public DynaWaltzParametersInfos() {
        name = EXTENSION_NAME;
    }

}
