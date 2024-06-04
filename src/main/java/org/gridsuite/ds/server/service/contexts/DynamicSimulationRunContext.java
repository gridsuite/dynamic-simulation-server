/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawaltz.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.Network;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.gridsuite.ds.server.computation.dto.ReportInfos;
import org.gridsuite.ds.server.computation.service.AbstractComputationRunContext;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;

import java.util.List;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
public class DynamicSimulationRunContext extends AbstractComputationRunContext<DynamicSimulationParametersInfos> {

    @Setter private String mapping;

    // network is cached in context to avoid re-loading in different methods of the worker service
    @Setter private Network network;

    // fields which are enriched in worker service
    @Setter private List<DynamicModelConfig> dynamicModelContent;

    @Setter private String eventModelContent;

    @Setter private String curveContent;

    @Setter private DynamicSimulationParameters dynamicSimulationParameters;

    @Builder
    public DynamicSimulationRunContext(UUID networkUuid, String variantId, String receiver, String provider, String mapping,
                                       ReportInfos reportInfos, String userId, DynamicSimulationParametersInfos parameters) {
        super(networkUuid, variantId, receiver, reportInfos, userId, provider, parameters, null);
        this.mapping = mapping;
    }
}

