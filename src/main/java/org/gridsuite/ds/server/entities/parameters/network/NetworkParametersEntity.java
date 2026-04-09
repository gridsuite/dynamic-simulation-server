/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.entities.parameters.network;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.ds.server.dto.network.NetworkInfos;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "network_parameters")
public class NetworkParametersEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "capacitor_no_reclosing_delay")
    private double capacitorNoReclosingDelay;

    @Column(name = "boundary_line_current_limit_max_time_operation")
    private double boundaryLineCurrentLimitMaxTimeOperation;

    @Column(name = "line_current_limit_max_time_operation")
    private double lineCurrentLimitMaxTimeOperation;

    @Column(name = "load_tp")
    private double loadTp;

    @Column(name = "load_tq")
    private double loadTq;

    @Column(name = "load_alpha")
    private double loadAlpha;

    @Column(name = "load_alpha_long")
    private double loadAlphaLong;

    @Column(name = "load_beta")
    private double loadBeta;

    @Column(name = "load_beta_long")
    private double loadBetaLong;

    @Column(name = "load_is_controllable")
    private boolean loadIsControllable;

    @Column(name = "load_is_restorative")
    private boolean loadIsRestorative;

    @Column(name = "load_zp_max")
    private double loadZPMax;

    @Column(name = "load_zq_max")
    private double loadZQMax;

    @Column(name = "reactance_no_reclosing_delay")
    private double reactanceNoReclosingDelay;

    @Column(name = "transformer_current_limit_max_time_operation")
    private double transformerCurrentLimitMaxTimeOperation;

    @Column(name = "transformer_t1st_ht")
    private double transformerT1StHT;

    @Column(name = "transformer_t1st_tht")
    private double transformerT1StTHT;

    @Column(name = "transformer_t_next_ht")
    private double transformerTNextHT;

    @Column(name = "transformer_t_next_tht")
    private double transformerTNextTHT;

    @Column(name = "transformer_tol_v")
    private double transformerTolV;

    public NetworkParametersEntity(NetworkInfos networkInfos) {
        assignAttributes(networkInfos);
    }

    public void assignAttributes(NetworkInfos networkInfos) {
        if (id == null) {
            id = UUID.randomUUID();
        }
        capacitorNoReclosingDelay = networkInfos.getCapacitorNoReclosingDelay();
        boundaryLineCurrentLimitMaxTimeOperation = networkInfos.getBoundaryLineCurrentLimitMaxTimeOperation();
        lineCurrentLimitMaxTimeOperation = networkInfos.getLineCurrentLimitMaxTimeOperation();
        loadTp = networkInfos.getLoadTp();
        loadTq = networkInfos.getLoadTq();
        loadAlpha = networkInfos.getLoadAlpha();
        loadAlphaLong = networkInfos.getLoadAlphaLong();
        loadBeta = networkInfos.getLoadBeta();
        loadBetaLong = networkInfos.getLoadBetaLong();
        loadIsControllable = networkInfos.isLoadIsControllable();
        loadIsRestorative = networkInfos.isLoadIsRestorative();
        loadZPMax = networkInfos.getLoadZPMax();
        loadZQMax = networkInfos.getLoadZQMax();
        reactanceNoReclosingDelay = networkInfos.getReactanceNoReclosingDelay();
        transformerCurrentLimitMaxTimeOperation = networkInfos.getTransformerCurrentLimitMaxTimeOperation();
        transformerT1StHT = networkInfos.getTransformerT1StHT();
        transformerT1StTHT = networkInfos.getTransformerT1StTHT();
        transformerTNextHT = networkInfos.getTransformerTNextHT();
        transformerTNextTHT = networkInfos.getTransformerTNextTHT();
        transformerTolV = networkInfos.getTransformerTolV();
    }

    public void update(NetworkInfos networkInfos) {
        assignAttributes(networkInfos);
    }

    public NetworkInfos toDto(boolean toDuplicate) {
        NetworkInfos networkInfos = new NetworkInfos();
        networkInfos.setId(toDuplicate ? null : id);
        networkInfos.setCapacitorNoReclosingDelay(capacitorNoReclosingDelay);
        networkInfos.setBoundaryLineCurrentLimitMaxTimeOperation(boundaryLineCurrentLimitMaxTimeOperation);
        networkInfos.setLineCurrentLimitMaxTimeOperation(lineCurrentLimitMaxTimeOperation);
        networkInfos.setLoadTp(loadTp);
        networkInfos.setLoadTq(loadTq);
        networkInfos.setLoadAlpha(loadAlpha);
        networkInfos.setLoadAlphaLong(loadAlphaLong);
        networkInfos.setLoadBeta(loadBeta);
        networkInfos.setLoadBetaLong(loadBetaLong);
        networkInfos.setLoadIsControllable(loadIsControllable);
        networkInfos.setLoadIsRestorative(loadIsRestorative);
        networkInfos.setLoadZPMax(loadZPMax);
        networkInfos.setLoadZQMax(loadZQMax);
        networkInfos.setReactanceNoReclosingDelay(reactanceNoReclosingDelay);
        networkInfos.setTransformerCurrentLimitMaxTimeOperation(transformerCurrentLimitMaxTimeOperation);
        networkInfos.setTransformerT1StHT(transformerT1StHT);
        networkInfos.setTransformerT1StTHT(transformerT1StTHT);
        networkInfos.setTransformerTNextHT(transformerTNextHT);
        networkInfos.setTransformerTNextTHT(transformerTNextTHT);
        networkInfos.setTransformerTolV(transformerTolV);
        return networkInfos;
    }
}
