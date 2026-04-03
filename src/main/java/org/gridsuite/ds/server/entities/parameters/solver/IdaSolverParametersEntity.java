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
import org.gridsuite.ds.server.dto.solver.IdaSolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@DiscriminatorValue("IDA")
public class IdaSolverParametersEntity extends SolverParametersEntity {

    @Column(name = "order_value")
    private int order;

    @Column(name = "init_step")
    private double initStep;

    @Column(name = "min_step")
    private double minStep;

    @Column(name = "max_step")
    private double maxStep;

    @Column(name = "abs_accuracy")
    private double absAccuracy;

    @Column(name = "rel_accuracy")
    private double relAccuracy;

    public IdaSolverParametersEntity(IdaSolverInfos solverInfos) {
        assignAttributes(solverInfos);
    }

    public void assignAttributes(IdaSolverInfos solverInfos) {
        super.assignAttributes(solverInfos);
        order = solverInfos.getOrder();
        initStep = solverInfos.getInitStep();
        minStep = solverInfos.getMinStep();
        maxStep = solverInfos.getMaxStep();
        absAccuracy = solverInfos.getAbsAccuracy();
        relAccuracy = solverInfos.getRelAccuracy();
    }

    @Override
    public void update(SolverInfos solverInfos) {
        assignAttributes((IdaSolverInfos) solverInfos);
    }

    @Override
    public SolverInfos toDto(boolean toDuplicate) {
        IdaSolverInfos dto = IdaSolverInfos.builder()
                .order(order)
                .initStep(initStep)
                .minStep(minStep)
                .maxStep(maxStep)
                .absAccuracy(absAccuracy)
                .relAccuracy(relAccuracy)
                .build();
        super.fillDto(dto, toDuplicate);
        return dto;
    }
}
