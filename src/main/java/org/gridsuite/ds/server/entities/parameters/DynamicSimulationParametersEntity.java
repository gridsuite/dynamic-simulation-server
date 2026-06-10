/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.entities.parameters;

import com.powsybl.dynawo.DynawoSimulationParameters.SolverType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.network.NetworkInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;
import org.gridsuite.ds.server.entities.parameters.curve.CurveEntity;
import org.gridsuite.ds.server.entities.parameters.network.NetworkParametersEntity;
import org.gridsuite.ds.server.entities.parameters.solver.SolverParametersEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "dynamic_simulation_parameters")
public class DynamicSimulationParametersEntity {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "provider")
    private String provider;

    @Column(name = "start_time")
    private double startTime;

    @Column(name = "stop_time")
    private double stopTime;

    @Column(name = "solver_id")
    @Enumerated(EnumType.STRING)
    private SolverType solver;

    @Column(name = "mapping_id")
    private UUID mappingId;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "dynamic_simulation_parameters_id",
            foreignKey = @ForeignKey(name = "solver_parameters_dynamic_simulation_parameters_id_fk"))
    @OrderColumn(name = "pos")
    private List<SolverParametersEntity> solvers = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "network_parameters_id",
            foreignKey = @ForeignKey(name = "dynamic_simulation_parameters_network_parameters_id_fk"))
    private NetworkParametersEntity network;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "dynamic_simulation_parameters_id",
            foreignKey = @ForeignKey(name = "curve_dynamic_simulation_parameters_id_fk"))
    @OrderColumn(name = "pos")
    private List<CurveEntity> curves = new ArrayList<>();

    public DynamicSimulationParametersEntity(DynamicSimulationParametersInfos parametersInfos) {
        assignAttributes(parametersInfos);
    }

    public void assignAttributes(DynamicSimulationParametersInfos parametersInfos) {
        if (id == null) {
            id = UUID.randomUUID();
        }
        provider = parametersInfos.getProvider();
        startTime = parametersInfos.getStartTime() != null ? parametersInfos.getStartTime() : 0;
        stopTime = parametersInfos.getStopTime() != null ? parametersInfos.getStopTime() : 0;
        mappingId = parametersInfos.getMappingId();
        solver = parametersInfos.getSolver();

        // --- Solvers ---
        assignSolvers(parametersInfos.getSolvers());

        // --- Network ---
        assignNetwork(parametersInfos.getNetwork());

        // --- Curves ---
        assignCurves(parametersInfos.getCurves());
    }

    private void assignSolvers(List<SolverInfos> solverInfosList) {
        if (CollectionUtils.isEmpty(solverInfosList)) {
            solvers.clear();
            return;
        }

        // build existing solvers Map
        Map<UUID, SolverParametersEntity> solversByIdMap = solvers.stream().collect(Collectors.toMap(SolverParametersEntity::getId, solverEntity -> solverEntity));

        // merge existing and add new solvers
        List<SolverParametersEntity> mergedSolvers = new ArrayList<>();
        for (SolverInfos solverInfos : solverInfosList) {
            if (solverInfos.getId() != null) {
                SolverParametersEntity existingEntity = solversByIdMap.get(solverInfos.getId());
                existingEntity.update(solverInfos);
                mergedSolvers.add(existingEntity);
            } else {
                mergedSolvers.add(SolverParametersEntity.fromDto(solverInfos));
            }
        }

        // by clear/addAll, existing elements that are not present in the new list will be removed systematically
        solvers.clear();
        solvers.addAll(mergedSolvers);
    }

    private void assignNetwork(NetworkInfos networkInfos) {
        if (networkInfos == null) {
            network = null;
            return;
        }

        if (networkInfos.getId() != null) {
            network.update(networkInfos);
        } else {
            network = new NetworkParametersEntity(networkInfos);
        }
    }

    private void assignCurves(List<CurveInfos> curveInfosList) {
        if (CollectionUtils.isEmpty(curveInfosList)) {
            curves.clear();
            return;
        }

        // build existing curves Map
        Map<UUID, CurveEntity> curvesByIdMap = curves.stream().collect(Collectors.toMap(CurveEntity::getId, curveEntity -> curveEntity));

        // merge existing and add new curves
        List<CurveEntity> mergedCurves = new ArrayList<>();
        for (CurveInfos curveInfos : curveInfosList) {
            if (curveInfos.getId() != null) {
                CurveEntity existingEntity = curvesByIdMap.get(curveInfos.getId());
                existingEntity.update(curveInfos);
                mergedCurves.add(existingEntity);
            } else {
                mergedCurves.add(new CurveEntity(curveInfos));
            }
        }

        // by clear/addAll, existing elements that are not present in the new list will be removed systematically
        curves.clear();
        curves.addAll(mergedCurves);
    }

    public void update(DynamicSimulationParametersInfos parametersInfos) {
        assignAttributes(parametersInfos);
    }

    public DynamicSimulationParametersInfos toDto(boolean toDuplicate) {
        DynamicSimulationParametersInfos dto = new DynamicSimulationParametersInfos();
        dto.setId(toDuplicate ? null : id);
        dto.setProvider(provider);
        dto.setStartTime(startTime);
        dto.setStopTime(stopTime);
        dto.setMappingId(mappingId);
        dto.setSolver(solver);

        dto.setSolvers(solvers != null
                ? solvers.stream().map(solverEntity -> solverEntity.toDto(toDuplicate)).collect(Collectors.toList())
                : Collections.emptyList());

        dto.setNetwork(network != null ? network.toDto(toDuplicate) : null);

        dto.setCurves(curves != null
                ? curves.stream().map(curve -> curve.toDto(toDuplicate)).collect(Collectors.toList())
                : Collections.emptyList());

        return dto;
    }
}
