/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.entities.parameters.curve;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.utils.EquipmentType;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "curve", indexes = @Index(name = "idx_curve_dynamic_simulation_parameters_id",
        columnList = "dynamic_simulation_parameters_id"))
public class CurveEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "equipment_type")
    @Enumerated(EnumType.STRING)
    private EquipmentType equipmentType;

    @Column(name = "equipment_id")
    private String equipmentId;

    @Column(name = "variable_id")
    private String variableId;

    public CurveEntity(CurveInfos curveInfos) {
        assignAttributes(curveInfos);
    }

    public void assignAttributes(CurveInfos curveInfos) {
        if (id == null) {
            id = UUID.randomUUID();
        }
        equipmentType = curveInfos.getEquipmentType();
        equipmentId = curveInfos.getEquipmentId();
        variableId = curveInfos.getVariableId();
    }

    public void update(CurveInfos curveInfos) {
        assignAttributes(curveInfos);
    }

    public CurveInfos toDto(boolean toDuplicate) {
        CurveInfos curveInfos = new CurveInfos();
        curveInfos.setId(toDuplicate ? null : id);
        curveInfos.setEquipmentType(equipmentType);
        curveInfos.setEquipmentId(equipmentId);
        curveInfos.setVariableId(variableId);
        return curveInfos;
    }
}
