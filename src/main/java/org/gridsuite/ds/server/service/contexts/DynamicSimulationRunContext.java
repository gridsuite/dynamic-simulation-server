/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.events.EventModelConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.computation.service.AbstractComputationRunContext;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
@Setter
public class DynamicSimulationRunContext extends AbstractComputationRunContext<DynamicSimulationParametersInfos> {

    private String mapping;

    // --- Fields which are enriched in worker service --- //

    private Path workDir;

    private List<DynamicModelConfig> t0DynamicModelContent;
    private List<DynamicModelConfig> t1DynamicModelContent;

    private List<EventModelConfig> eventModelContent;

    private String curveContent;

    private DynamicSimulationParameters t0DynamicSimulationParameters;
    private DynamicSimulationParameters t1DynamicSimulationParameters;

    @Builder
    public DynamicSimulationRunContext(UUID networkUuid, String variantId, String receiver, String provider, String mapping,
                                       ReportInfos reportInfos, String userId, DynamicSimulationParametersInfos parameters) {
        super(networkUuid, variantId, receiver, reportInfos, userId, provider, parameters);
        this.mapping = mapping;
    }
}

