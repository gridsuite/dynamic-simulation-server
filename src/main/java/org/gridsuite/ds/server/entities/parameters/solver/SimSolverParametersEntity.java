/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.entities.parameters.solver;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.ds.server.dto.solver.SimSolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@DiscriminatorValue("SIM")
public class SimSolverParametersEntity extends SolverParametersEntity {

    @Column(name = "h_min")
    private double hMin;

    @Column(name = "h_max")
    private double hMax;

    @Column(name = "k_reduce_step")
    private double kReduceStep;

    @Column(name = "max_newton_try")
    private int maxNewtonTry;

    @Column(name = "linear_solver_name")
    private String linearSolverName;

    @Column(name = "f_norm_tol")
    private double fNormTol;

    @Column(name = "initial_add_tol")
    private double initialAddTol;

    @Column(name = "sc_step_tol")
    private double scStepTol;

    @Column(name = "mx_new_t_step")
    private double mxNewTStep;

    @Column(name = "msbset")
    private int msbset;

    @Column(name = "mx_iter")
    private int mxIter;

    @Column(name = "print_fl")
    private int printFl;

    @Column(name = "optimize_algebraic_residuals_evaluations")
    private boolean optimizeAlgebraicResidualsEvaluations;

    @Column(name = "skip_nr_if_initial_guess_ok")
    private boolean skipNRIfInitialGuessOK;

    @Column(name = "enable_silent_z")
    private boolean enableSilentZ;

    @Column(name = "optimize_reinit_algebraic_residuals_evaluations")
    private boolean optimizeReInitAlgebraicResidualsEvaluations;

    @Column(name = "min_mode_change_type_alg_restoration")
    private String minimumModeChangeTypeForAlgebraicRestoration;

    @Column(name = "min_mode_change_type_alg_restoration_init")
    private String minimumModeChangeTypeForAlgebraicRestorationInit;

    public SimSolverParametersEntity(SimSolverInfos solverInfos) {
        assignAttributes(solverInfos);
    }

    public void assignAttributes(SimSolverInfos solverInfos) {
        super.assignAttributes(solverInfos);
        hMin = solverInfos.getHMin();
        hMax = solverInfos.getHMax();
        kReduceStep = solverInfos.getKReduceStep();
        maxNewtonTry = solverInfos.getMaxNewtonTry();
        linearSolverName = solverInfos.getLinearSolverName();
        fNormTol = solverInfos.getFNormTol();
        initialAddTol = solverInfos.getInitialAddTol();
        scStepTol = solverInfos.getScStepTol();
        mxNewTStep = solverInfos.getMxNewTStep();
        msbset = solverInfos.getMsbset();
        mxIter = solverInfos.getMxIter();
        printFl = solverInfos.getPrintFl();
        optimizeAlgebraicResidualsEvaluations = solverInfos.isOptimizeAlgebraicResidualsEvaluations();
        skipNRIfInitialGuessOK = solverInfos.isSkipNRIfInitialGuessOK();
        enableSilentZ = solverInfos.isEnableSilentZ();
        optimizeReInitAlgebraicResidualsEvaluations = solverInfos.isOptimizeReInitAlgebraicResidualsEvaluations();
        minimumModeChangeTypeForAlgebraicRestoration = solverInfos.getMinimumModeChangeTypeForAlgebraicRestoration();
        minimumModeChangeTypeForAlgebraicRestorationInit = solverInfos.getMinimumModeChangeTypeForAlgebraicRestorationInit();
    }

    @Override
    public void update(SolverInfos solverInfos) {
        assignAttributes((SimSolverInfos) solverInfos);
    }

    @Override
    public SolverInfos toDto(boolean toDuplicate) {
        SimSolverInfos dto = SimSolverInfos.builder()
                .hMin(hMin)
                .hMax(hMax)
                .kReduceStep(kReduceStep)
                .maxNewtonTry(maxNewtonTry)
                .linearSolverName(linearSolverName)
                .fNormTol(fNormTol)
                .initialAddTol(initialAddTol)
                .scStepTol(scStepTol)
                .mxNewTStep(mxNewTStep)
                .msbset(msbset)
                .mxIter(mxIter)
                .printFl(printFl)
                .optimizeAlgebraicResidualsEvaluations(optimizeAlgebraicResidualsEvaluations)
                .skipNRIfInitialGuessOK(skipNRIfInitialGuessOK)
                .enableSilentZ(enableSilentZ)
                .optimizeReInitAlgebraicResidualsEvaluations(optimizeReInitAlgebraicResidualsEvaluations)
                .minimumModeChangeTypeForAlgebraicRestoration(minimumModeChangeTypeForAlgebraicRestoration)
                .minimumModeChangeTypeForAlgebraicRestorationInit(minimumModeChangeTypeForAlgebraicRestorationInit)
                .build();
        super.fillDto(dto, toDuplicate);
        return dto;
    }
}
