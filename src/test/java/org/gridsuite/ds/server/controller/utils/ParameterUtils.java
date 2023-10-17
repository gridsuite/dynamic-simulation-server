/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.controller.utils;

import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.event.EventPropertyInfos;
import org.gridsuite.ds.server.dto.solver.IdaSolverInfos;
import org.gridsuite.ds.server.dto.solver.SimSolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverTypeInfos;
import org.gridsuite.ds.server.dto.network.NetworkInfos;
import org.gridsuite.ds.server.utils.PropertyType;

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

        // solvers
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

        // network
        NetworkInfos network = new NetworkInfos();
        network.setCapacitorNoReclosingDelay(300);
        network.setDanglingLineCurrentLimitMaxTimeOperation(90);
        network.setLineCurrentLimitMaxTimeOperation(90);
        network.setLoadTp(90);
        network.setLoadTq(90);
        network.setLoadAlpha(1);
        network.setLoadAlphaLong(0);
        network.setLoadBeta(2);
        network.setLoadBetaLong(0);
        network.setLoadIsControllable(false);
        network.setLoadIsRestorative(false);
        network.setLoadZPMax(100);
        network.setLoadZQMax(100);
        network.setReactanceNoReclosingDelay(0);
        network.setTransformerCurrentLimitMaxTimeOperation(90);
        network.setTransformerT1StHT(60);
        network.setTransformerT1StTHT(30);
        network.setTransformerTNextHT(10);
        network.setTransformerTNextTHT(10);
        network.setTransformerTolV(0.015);

        parameters.setNetwork(network);

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

    public static List<EventInfos> getEventInfosList() {
        return List.of(
                new EventInfos(null, null, "_BUS____1-BUS____5-1_AC", null, "Disconnect", List.of(
                        new EventPropertyInfos(null, "staticId", "_BUS____1-BUS____5-1_AC", PropertyType.STRING),
                        new EventPropertyInfos(null, "startTime", "1", PropertyType.FLOAT),
                        new EventPropertyInfos(null, "disconnectOnly", "Branch.Side.TWO", PropertyType.ENUM)
                ))
        );
    }
}
