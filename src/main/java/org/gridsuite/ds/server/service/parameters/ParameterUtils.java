/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.network.NetworkInfos;
import org.gridsuite.ds.server.dto.solver.IdaSolverInfos;
import org.gridsuite.ds.server.dto.solver.SimSolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ParameterUtils {
    private ParameterUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static DynamicSimulationParametersInfos getDefaultParametersValues() {
        DynamicSimulationParameters defaultConfigParameters = DynamicSimulationParameters.load();

        IdaSolverInfos idaSolver = getDefaultIdaSolverValues();
        SimSolverInfos simSolver = getDefaultSimSolverValues();
        List<SolverInfos> solvers = List.of(idaSolver, simSolver);

        NetworkInfos network = getDefaultNetworkValues();

        return DynamicSimulationParametersInfos.builder()
                .provider(DynawoSimulationProvider.NAME)
                .startTime(defaultConfigParameters.getStartTime())
                .stopTime(defaultConfigParameters.getStopTime())
                .solver(DynawoSimulationParameters.SolverType.IDA)
                .solvers(solvers)
                .network(network)
                .build();
    }

    public static IdaSolverInfos getDefaultIdaSolverValues() {
        IdaSolverInfos idaSolver = new IdaSolverInfos();

        // we do not yet support getting values from DynawoSimulationParameters.load()
        // due to the lack of deserialization from the solvers.par file to the dto
        idaSolver.setType(DynawoSimulationParameters.SolverType.IDA);
        idaSolver.setOrder(2);
        idaSolver.setInitStep(1.e-7);
        idaSolver.setMinStep(1.e-7);
        idaSolver.setMaxStep(10);
        idaSolver.setAbsAccuracy(1.e-4);
        idaSolver.setRelAccuracy(1.e-4);

        idaSolver.setFNormTolAlg(1.e-4);
        idaSolver.setInitialAddTolAlg(1);
        idaSolver.setScStepTolAlg(1.e-4);
        idaSolver.setMxNewTStepAlg(10000);
        idaSolver.setMsbsetAlg(5);
        idaSolver.setMxIterAlg(30);
        idaSolver.setPrintFlAlg(0);
        idaSolver.setFNormTolAlgJ(1.e-4);
        idaSolver.setInitialAddTolAlgJ(1);
        idaSolver.setScStepTolAlgJ(1.e-4);
        idaSolver.setMxNewTStepAlgJ(10000);
        idaSolver.setMsbsetAlgJ(1);
        idaSolver.setMxIterAlgJ(50);
        idaSolver.setPrintFlAlgJ(0);
        idaSolver.setFNormTolAlgInit(1.e-4);
        idaSolver.setInitialAddTolAlgInit(1);
        idaSolver.setScStepTolAlgInit(1.e-4);
        idaSolver.setMxNewTStepAlgInit(10000);
        idaSolver.setMsbsetAlgInit(1);
        idaSolver.setMxIterAlgInit(50);
        idaSolver.setPrintFlAlgInit(0);
        idaSolver.setMinimalAcceptableStep(1.e-8);
        idaSolver.setMaximumNumberSlowStepIncrease(40);

        return idaSolver;
    }

    public static SimSolverInfos getDefaultSimSolverValues() {
        SimSolverInfos simSolver = new SimSolverInfos();

        // we do not yet support getting values from DynawoSimulationParameters.load()
        // due to the lack of deserialization from the solvers.par file to the dto
        simSolver.setType(DynawoSimulationParameters.SolverType.SIM);
        simSolver.setHMin(0.001);
        simSolver.setHMax(1);
        simSolver.setKReduceStep(0.5);
        simSolver.setMaxNewtonTry(10);
        simSolver.setLinearSolverName("KLU");

        simSolver.setFNormTol(1.e-3);
        simSolver.setInitialAddTol(1);
        simSolver.setScStepTol(1.e-3);
        simSolver.setMxNewTStep(10000);
        simSolver.setMsbset(0);
        simSolver.setMxIter(15);
        simSolver.setPrintFl(0);
        simSolver.setOptimizeAlgebraicResidualsEvaluations(true);
        simSolver.setSkipNRIfInitialGuessOK(true);
        simSolver.setEnableSilentZ(true);
        simSolver.setOptimizeReInitAlgebraicResidualsEvaluations(true);
        simSolver.setMinimumModeChangeTypeForAlgebraicRestoration("ALGEBRAIC_J_UPDATE");
        simSolver.setMinimumModeChangeTypeForAlgebraicRestorationInit("ALGEBRAIC_J_UPDATE");

        simSolver.setFNormTolAlg(1.e-3);
        simSolver.setInitialAddTolAlg(1);
        simSolver.setScStepTolAlg(1.e-3);
        simSolver.setMxNewTStepAlg(10000);
        simSolver.setMsbsetAlg(5);
        simSolver.setMxIterAlg(30);
        simSolver.setPrintFlAlg(0);
        simSolver.setFNormTolAlgJ(1.e-3);
        simSolver.setInitialAddTolAlgJ(1);
        simSolver.setScStepTolAlgJ(1.e-3);
        simSolver.setMxNewTStepAlgJ(10000);
        simSolver.setMsbsetAlgJ(1);
        simSolver.setMxIterAlgJ(50);
        simSolver.setPrintFlAlgJ(0);
        simSolver.setFNormTolAlgInit(1.e-3);
        simSolver.setInitialAddTolAlgInit(1);
        simSolver.setScStepTolAlgInit(1.e-3);
        simSolver.setMxNewTStepAlgInit(10000);
        simSolver.setMsbsetAlgInit(1);
        simSolver.setMxIterAlgInit(50);
        simSolver.setPrintFlAlgInit(0);
        simSolver.setMinimalAcceptableStep(1.e-3);
        simSolver.setMaximumNumberSlowStepIncrease(40);

        return simSolver;
    }

    public static NetworkInfos getDefaultNetworkValues() {
        NetworkInfos network = new NetworkInfos();

        // we do not yet support getting values from DynawoSimulationParameters.load()
        // due to the lack of deserialization from the network.par file to the dto
        network.setCapacitorNoReclosingDelay(300);
        network.setBoundaryLineCurrentLimitMaxTimeOperation(240);
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
}
