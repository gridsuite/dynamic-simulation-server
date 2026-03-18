/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.entities.parameters.solver;

import com.powsybl.dynawo.DynawoSimulationParameters.SolverType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.ds.server.dto.solver.AbstractSolverInfos;
import org.gridsuite.ds.server.dto.solver.IdaSolverInfos;
import org.gridsuite.ds.server.dto.solver.SimSolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@SuppressWarnings("checkstyle:AbstractClassName")
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "solver_parameters", indexes = @Index(name = "idx_solver_parameters_dynamic_simulation_parameters_id",
        columnList = "dynamic_simulation_parameters_id"))
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "solver_type", discriminatorType = DiscriminatorType.STRING)
public abstract class SolverParametersEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "solver_type", insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private SolverType type;

    // --- Common fields from AbstractSolverInfos ---
    @Column(name = "f_norm_tol_alg")
    private double fNormTolAlg;

    @Column(name = "initial_add_tol_alg")
    private double initialAddTolAlg;

    @Column(name = "sc_step_tol_alg")
    private double scStepTolAlg;

    @Column(name = "mx_new_t_step_alg")
    private double mxNewTStepAlg;

    @Column(name = "msbset_alg")
    private int msbsetAlg;

    @Column(name = "mx_iter_alg")
    private int mxIterAlg;

    @Column(name = "print_fl_alg")
    private int printFlAlg;

    @Column(name = "f_norm_tol_alg_j")
    private double fNormTolAlgJ;

    @Column(name = "initial_add_tol_alg_j")
    private double initialAddTolAlgJ;

    @Column(name = "sc_step_tol_alg_j")
    private double scStepTolAlgJ;

    @Column(name = "mx_new_t_step_alg_j")
    private double mxNewTStepAlgJ;

    @Column(name = "msbset_alg_j")
    private int msbsetAlgJ;

    @Column(name = "mx_iter_alg_j")
    private int mxIterAlgJ;

    @Column(name = "print_fl_alg_j")
    private int printFlAlgJ;

    @Column(name = "f_norm_tol_alg_init")
    private double fNormTolAlgInit;

    @Column(name = "initial_add_tol_alg_init")
    private double initialAddTolAlgInit;

    @Column(name = "sc_step_tol_alg_init")
    private double scStepTolAlgInit;

    @Column(name = "mx_new_t_step_alg_init")
    private double mxNewTStepAlgInit;

    @Column(name = "msbset_alg_init")
    private int msbsetAlgInit;

    @Column(name = "mx_iter_alg_init")
    private int mxIterAlgInit;

    @Column(name = "print_fl_alg_init")
    private int printFlAlgInit;

    @Column(name = "max_number_slow_step_increase")
    private int maximumNumberSlowStepIncrease;

    @Column(name = "minimal_acceptable_step")
    private double minimalAcceptableStep;

    protected void assignAttributes(AbstractSolverInfos solverInfos) {
        if (id == null) {
            id = UUID.randomUUID();
        }
        type = solverInfos.getType();
        fNormTolAlg = solverInfos.getFNormTolAlg();
        initialAddTolAlg = solverInfos.getInitialAddTolAlg();
        scStepTolAlg = solverInfos.getScStepTolAlg();
        mxNewTStepAlg = solverInfos.getMxNewTStepAlg();
        msbsetAlg = solverInfos.getMsbsetAlg();
        mxIterAlg = solverInfos.getMxIterAlg();
        printFlAlg = solverInfos.getPrintFlAlg();
        fNormTolAlgJ = solverInfos.getFNormTolAlgJ();
        initialAddTolAlgJ = solverInfos.getInitialAddTolAlgJ();
        scStepTolAlgJ = solverInfos.getScStepTolAlgJ();
        mxNewTStepAlgJ = solverInfos.getMxNewTStepAlgJ();
        msbsetAlgJ = solverInfos.getMsbsetAlgJ();
        mxIterAlgJ = solverInfos.getMxIterAlgJ();
        printFlAlgJ = solverInfos.getPrintFlAlgJ();
        fNormTolAlgInit = solverInfos.getFNormTolAlgInit();
        initialAddTolAlgInit = solverInfos.getInitialAddTolAlgInit();
        scStepTolAlgInit = solverInfos.getScStepTolAlgInit();
        mxNewTStepAlgInit = solverInfos.getMxNewTStepAlgInit();
        msbsetAlgInit = solverInfos.getMsbsetAlgInit();
        mxIterAlgInit = solverInfos.getMxIterAlgInit();
        printFlAlgInit = solverInfos.getPrintFlAlgInit();
        maximumNumberSlowStepIncrease = solverInfos.getMaximumNumberSlowStepIncrease();
        minimalAcceptableStep = solverInfos.getMinimalAcceptableStep();
    }

    public void update(SolverInfos solverInfos) {
        assignAttributes((AbstractSolverInfos) solverInfos);
    }

    protected void fillDto(AbstractSolverInfos solverInfos, boolean toDuplicate) {
        solverInfos.setId(toDuplicate ? null : id);
        solverInfos.setType(type);
        solverInfos.setFNormTolAlg(fNormTolAlg);
        solverInfos.setInitialAddTolAlg(initialAddTolAlg);
        solverInfos.setScStepTolAlg(scStepTolAlg);
        solverInfos.setMxNewTStepAlg(mxNewTStepAlg);
        solverInfos.setMsbsetAlg(msbsetAlg);
        solverInfos.setMxIterAlg(mxIterAlg);
        solverInfos.setPrintFlAlg(printFlAlg);
        solverInfos.setFNormTolAlgJ(fNormTolAlgJ);
        solverInfos.setInitialAddTolAlgJ(initialAddTolAlgJ);
        solverInfos.setScStepTolAlgJ(scStepTolAlgJ);
        solverInfos.setMxNewTStepAlgJ(mxNewTStepAlgJ);
        solverInfos.setMsbsetAlgJ(msbsetAlgJ);
        solverInfos.setMxIterAlgJ(mxIterAlgJ);
        solverInfos.setPrintFlAlgJ(printFlAlgJ);
        solverInfos.setFNormTolAlgInit(fNormTolAlgInit);
        solverInfos.setInitialAddTolAlgInit(initialAddTolAlgInit);
        solverInfos.setScStepTolAlgInit(scStepTolAlgInit);
        solverInfos.setMxNewTStepAlgInit(mxNewTStepAlgInit);
        solverInfos.setMsbsetAlgInit(msbsetAlgInit);
        solverInfos.setMxIterAlgInit(mxIterAlgInit);
        solverInfos.setPrintFlAlgInit(printFlAlgInit);
        solverInfos.setMaximumNumberSlowStepIncrease(maximumNumberSlowStepIncrease);
        solverInfos.setMinimalAcceptableStep(minimalAcceptableStep);
    }

    public abstract SolverInfos toDto(boolean toDuplicate);

    public static SolverParametersEntity fromDto(SolverInfos solverInfos) {
        if (solverInfos instanceof IdaSolverInfos idaInfos) {
            return new IdaSolverParametersEntity(idaInfos);
        } else if (solverInfos instanceof SimSolverInfos simInfos) {
            return new SimSolverParametersEntity(simInfos);
        }
        throw new IllegalArgumentException("Unknown solver type: " + solverInfos.getType());
    }
}
