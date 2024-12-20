/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.solver;

import com.powsybl.dynawo.DynawoSimulationParameters;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public enum SolverTypeInfos {
    SIM,
    IDA;

    public DynawoSimulationParameters.SolverType toSolverType() {
        switch (this) {
            case SIM : return DynawoSimulationParameters.SolverType.SIM;
            case IDA : return DynawoSimulationParameters.SolverType.IDA;
            default : return null;
        }
    }
}
