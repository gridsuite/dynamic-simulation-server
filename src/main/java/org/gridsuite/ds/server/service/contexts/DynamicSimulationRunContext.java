/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import lombok.Builder;
import lombok.Getter;
import org.gridsuite.ds.server.computation.service.AbstractComputationRunContext;
import org.gridsuite.ds.server.computation.utils.ReportContext;
import org.gridsuite.ds.server.service.parameters.DynamicSimulationParametersValues;

import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
public class DynamicSimulationRunContext extends AbstractComputationRunContext<DynamicSimulationParametersValues> {

    private final String mapping;

    @Builder
    public DynamicSimulationRunContext(UUID networkUuid, String variantId, String receiver, String provider, String mapping, ReportContext reportContext, String userId, DynamicSimulationParametersValues parameters) {
        super(networkUuid, variantId, receiver, reportContext, userId, 0.0F, provider, parameters);
        this.mapping = mapping;
    }
}

