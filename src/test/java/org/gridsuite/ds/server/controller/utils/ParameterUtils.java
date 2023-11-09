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

    /**
     * get default dynamic simulation parameters
     * @return a default dynamic simulation parameters
     */
    public static DynamicSimulationParametersInfos getDefaultDynamicSimulationParameters() {
        IdaSolverInfos idaSolver = getDefaultIdaSolver();
        SimSolverInfos simSolver = getDefaultSimSolver();
        List<SolverInfos> solvers = List.of(idaSolver, simSolver);

        NetworkInfos network = getDefaultNetwork();
        return new DynamicSimulationParametersInfos(0.0, 50.0, idaSolver.getId(), solvers, network, null, null);
    }

    public static IdaSolverInfos getDefaultIdaSolver() {
        IdaSolverInfos idaSolver = new IdaSolverInfos();

        // these parameters are taken from solver.par file in dynamic simulation server
        idaSolver.setId("IDA");
        idaSolver.setType(SolverTypeInfos.IDA);
        idaSolver.setOrder(2);
        idaSolver.setInitStep(1.e-7);
        idaSolver.setMinStep(1.e-7);
        idaSolver.setMaxStep(10);
        idaSolver.setAbsAccuracy(1.e-4);
        idaSolver.setRelAccuracy(1.e-4);
        idaSolver.setFnormtolAlg(1.e-4);
        idaSolver.setInitialaddtolAlg(1);
        idaSolver.setScsteptolAlg(1.e-4);
        idaSolver.setMxnewtstepAlg(10000);
        idaSolver.setMsbsetAlg(5);
        idaSolver.setMxiterAlg(30);
        idaSolver.setPrintflAlg(0);
        idaSolver.setFnormtolAlgJ(1.e-4);
        idaSolver.setInitialaddtolAlgJ(1);
        idaSolver.setScsteptolAlgJ(1.e-4);
        idaSolver.setMxnewtstepAlgJ(10000);
        idaSolver.setMsbsetAlgJ(1);
        idaSolver.setMxiterAlgJ(50);
        idaSolver.setPrintflAlgJ(0);
        idaSolver.setFnormtolAlgInit(1.e-4);
        idaSolver.setInitialaddtolAlgInit(1);
        idaSolver.setScsteptolAlgInit(1.e-4);
        idaSolver.setMxnewtstepAlgInit(10000);
        idaSolver.setMsbsetAlgInit(1);
        idaSolver.setMxiterAlgInit(50);
        idaSolver.setPrintflAlgInit(0);
        idaSolver.setMinimalAcceptableStep(1.e-8);
        idaSolver.setMaximumNumberSlowStepIncrease(40);

        return idaSolver;
    }

    public static SimSolverInfos getDefaultSimSolver() {
        SimSolverInfos simSolver = new SimSolverInfos();

        // these parameters are taken from solver.par file in dynamic simulation server
        simSolver.setId("SIM");
        simSolver.setType(SolverTypeInfos.SIM);
        simSolver.setHMin(0.001);
        simSolver.setHMax(1);
        simSolver.setKReduceStep(0.5);
        simSolver.setMaxNewtonTry(10);
        simSolver.setLinearSolverName("KLU");
        simSolver.setFnormtol(1.e-3);
        simSolver.setInitialaddtol(1);
        simSolver.setScsteptol(1.e-3);
        simSolver.setMxnewtstep(10000);
        simSolver.setMsbset(0);
        simSolver.setMxiter(15);
        simSolver.setPrintfl(0);
        simSolver.setOptimizeAlgebraicResidualsEvaluations(true);
        simSolver.setSkipNRIfInitialGuessOK(true);
        simSolver.setEnableSilentZ(true);
        simSolver.setOptimizeReinitAlgebraicResidualsEvaluations(true);
        simSolver.setMinimumModeChangeTypeForAlgebraicRestoration("ALGEBRAIC_J_UPDATE");
        simSolver.setMinimumModeChangeTypeForAlgebraicRestorationInit("ALGEBRAIC_J_UPDATE");
        simSolver.setFnormtolAlg(1.e-3);
        simSolver.setInitialaddtolAlg(1);
        simSolver.setScsteptolAlg(1.e-3);
        simSolver.setMxnewtstepAlg(10000);
        simSolver.setMsbsetAlg(5);
        simSolver.setMxiterAlg(30);
        simSolver.setPrintflAlg(0);
        simSolver.setFnormtolAlgJ(1.e-3);
        simSolver.setInitialaddtolAlgJ(1);
        simSolver.setScsteptolAlgJ(1.e-3);
        simSolver.setMxnewtstepAlgJ(10000);
        simSolver.setMsbsetAlgJ(1);
        simSolver.setMxiterAlgJ(50);
        simSolver.setPrintflAlgJ(0);
        simSolver.setFnormtolAlgInit(1.e-3);
        simSolver.setInitialaddtolAlgInit(1);
        simSolver.setScsteptolAlgInit(1.e-3);
        simSolver.setMxnewtstepAlgInit(10000);
        simSolver.setMsbsetAlgInit(1);
        simSolver.setMxiterAlgInit(50);
        simSolver.setPrintflAlgInit(0);
        simSolver.setMinimalAcceptableStep(1.e-3);
        simSolver.setMaximumNumberSlowStepIncrease(40);

        return simSolver;
    }

    public static NetworkInfos getDefaultNetwork() {
        // these parameters are taken from network.par file in dynamic simulation server
        NetworkInfos network = new NetworkInfos();
        network.setCapacitorNoReclosingDelay(300);
        network.setDanglingLineCurrentLimitMaxTimeOperation(240);
        network.setLineCurrentLimitMaxTimeOperation(240);
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
        network.setTransformerCurrentLimitMaxTimeOperation(240);
        network.setTransformerT1StHT(60);
        network.setTransformerT1StTHT(30);
        network.setTransformerTNextHT(10);
        network.setTransformerTNextTHT(10);
        network.setTransformerTolV(0.015);

        return network;
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
