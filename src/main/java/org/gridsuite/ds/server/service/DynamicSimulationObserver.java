/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import lombok.NonNull;
import org.gridsuite.ds.server.computation.service.AbstractComputationObserver;
import org.gridsuite.ds.server.service.parameters.DynamicSimulationParametersValues;
import org.springframework.stereotype.Service;

import static org.gridsuite.ds.server.service.DynamicSimulationService.COMPUTATION_TYPE;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class DynamicSimulationObserver extends AbstractComputationObserver<DynamicSimulationResult, DynamicSimulationParametersValues> {

    public DynamicSimulationObserver(@NonNull ObservationRegistry observationRegistry, @NonNull MeterRegistry meterRegistry) {
        super(observationRegistry, meterRegistry);
    }

    @Override
    protected String getComputationType() {
        return COMPUTATION_TYPE;
    }

    @Override
    protected String getResultStatus(DynamicSimulationResult res) {
        return res != null && res.getStatus() == DynamicSimulationResult.Status.SUCCESS ? "OK" : "NOK";
    }
}
