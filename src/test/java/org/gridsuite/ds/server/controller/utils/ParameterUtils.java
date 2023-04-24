/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.controller.utils;

import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.solver.IdaSolverInfos;
import org.gridsuite.ds.server.dto.solver.SimSolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverTypeInfos;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ParameterUtils {
    private ParameterUtils() {

    }

    public static DynamicSimulationParametersInfos getDynamicSimulationParameters() {
        DynamicSimulationParametersInfos parameters = new DynamicSimulationParametersInfos();
        parameters.setStartTime(0.0);
        parameters.setStopTime(50.0);

        IdaSolverInfos idaSolver = new IdaSolverInfos();
        idaSolver.setId("2");
        idaSolver.setType(SolverTypeInfos.IDA);
        idaSolver.setOrder(2);
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

        parameters.setSolverId(idaSolver.getId());
        parameters.setSolvers(solvers);

        return parameters;
    }

    public static List<CurveInfos> getCurveInfosList() {
        return List.of(
                new CurveInfos("_LOAD___2_EC", "load_PPu"),
                new CurveInfos("_LOAD___2_EC", "load_QPu"),
                new CurveInfos("_GEN____3_SM", "generator_omegaPu"),
                new CurveInfos("_GEN____3_SM", "generator_PGen"),
                new CurveInfos("_GEN____3_SM", "generator_QGen"),
                new CurveInfos("_GEN____3_SM", "generator_UStatorPu"),
                new CurveInfos("_GEN____3_SM", "voltageRegulator_EfdPu")
        );
    }
}
