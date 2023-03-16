/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.controller.utils;

import org.gridsuite.ds.server.dto.DynamicSimulationExtension;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.dynawaltz.DynaWaltzParametersInfos;
import org.gridsuite.ds.server.dto.dynawaltz.solver.IdaSolverInfos;
import org.gridsuite.ds.server.dto.dynawaltz.solver.SimSolverInfos;
import org.gridsuite.ds.server.dto.dynawaltz.solver.SolverInfos;
import org.gridsuite.ds.server.dto.dynawaltz.solver.SolverTypeInfos;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ParameterUtils {
    private ParameterUtils() {

    }

    public static DynamicSimulationParametersInfos getDynaWaltzParameters() {
        DynamicSimulationParametersInfos parameters = new DynamicSimulationParametersInfos();
        parameters.setStartTime(0);
        parameters.setStopTime(50);

        IdaSolverInfos idaSolver = new IdaSolverInfos();
        idaSolver.setId("1");
        idaSolver.setType(SolverTypeInfos.IDA);
        idaSolver.setOrder(1);
        idaSolver.setInitStep(0.000001);
        idaSolver.setMinStep(0.000001);
        idaSolver.setMaxStep(10);
        idaSolver.setAbsAccuracy(0.0001);
        idaSolver.setRelAccuracy(0.0001);

        SimSolverInfos simSolver = new SimSolverInfos();
        simSolver.setId("3");
        simSolver.setType(SolverTypeInfos.SIM);
        simSolver.setHMin(0.000001);
        simSolver.setHMax(1);
        simSolver.setKReduceStep(0.5);
        simSolver.setNEff(10);
        simSolver.setNDeadband(2);
        simSolver.setMaxRootRestart(3);
        simSolver.setMaxNewtonTry(10);
        simSolver.setLinearSolverName("KLU");
        simSolver.setRecalculateStep(false);

        List<SolverInfos> solvers = List.of(idaSolver, simSolver);
        DynaWaltzParametersInfos dynaWaltzParametersInfos = new DynaWaltzParametersInfos(DynaWaltzParametersInfos.EXTENSION_NAME, solvers.get(0).getId(), solvers);
        List<DynamicSimulationExtension> extensions = List.of(dynaWaltzParametersInfos);

        parameters.setExtensions(extensions);
        return parameters;
    }
}
