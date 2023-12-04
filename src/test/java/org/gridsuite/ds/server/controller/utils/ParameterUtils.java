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
import org.gridsuite.ds.server.utils.EquipmentType;
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

    public static SimSolverInfos getDefaultSimSolver() {
        SimSolverInfos simSolver = new SimSolverInfos();

        simSolver.setId("SIM");
        simSolver.setType(SolverTypeInfos.SIM);
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

    public static NetworkInfos getDefaultNetwork() {
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
                new CurveInfos(EquipmentType.LOAD, "_LOAD___2_EC", "load_PPu"),
                new CurveInfos(EquipmentType.LOAD, "_LOAD___2_EC", "load_QPu"),
                new CurveInfos(EquipmentType.GENERATOR, "_GEN____3_SM", "generator_omegaPu"),
                new CurveInfos(EquipmentType.GENERATOR, "_GEN____3_SM", "generator_PGen"),
                new CurveInfos(EquipmentType.GENERATOR, "_GEN____3_SM", "generator_QGen"),
                new CurveInfos(EquipmentType.GENERATOR, "_GEN____3_SM", "generator_UStatorPu"),
                new CurveInfos(EquipmentType.GENERATOR, "_GEN____3_SM", "voltageRegulator_EfdPu"),
                new CurveInfos(EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_UPu"),
                new CurveInfos(EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_PInjPu"),
                new CurveInfos(EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_QInjPu"),
                new CurveInfos(EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_BPu"),
                new CurveInfos(EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_modeHandling_mode_value")
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
