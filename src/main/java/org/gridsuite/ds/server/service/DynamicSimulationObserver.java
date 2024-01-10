/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.NonNull;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.springframework.stereotype.Service;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Service
public class DynamicSimulationObserver {
    private static final String OBSERVATION_PREFIX = "app.computation.";

    private static final String PROVIDER_TAG_NAME = "provider";
    private static final String TYPE_TAG_NAME = "type";
    private static final String STATUS_TAG_NAME = "status";

    private static final String COMPUTATION_TYPE = "dynamicSimulation";

    private static final String COMPUTATION_COUNTER_NAME = OBSERVATION_PREFIX + "count";

    private final ObservationRegistry observationRegistry;

    private final MeterRegistry meterRegistry;

    public DynamicSimulationObserver(@NonNull ObservationRegistry observationRegistry, @NonNull MeterRegistry meterRegistry) {
        this.observationRegistry = observationRegistry;
        this.meterRegistry = meterRegistry;
    }

    public <E extends Throwable> void observe(String name, DynamicSimulationRunContext runContext, Observation.CheckedRunnable<E> runnable) throws E {
        createObservation(name, runContext).observeChecked(runnable);
    }

    public <T extends DynamicSimulationResult, E extends Throwable> T observeRun(String name, DynamicSimulationRunContext runContext, Observation.CheckedCallable<T, E> callable) throws E {
        T result = createObservation(name, runContext).observeChecked(callable);
        incrementCount(runContext, result);
        return result;
    }

    private Observation createObservation(String name, DynamicSimulationRunContext runContext) {
        return Observation.createNotStarted(OBSERVATION_PREFIX + name, observationRegistry)
                .lowCardinalityKeyValue(PROVIDER_TAG_NAME, runContext.getProvider())
                .lowCardinalityKeyValue(TYPE_TAG_NAME, COMPUTATION_TYPE);
    }

    private void incrementCount(DynamicSimulationRunContext runContext, DynamicSimulationResult result) {
        Counter.builder(COMPUTATION_COUNTER_NAME)
                .tag(PROVIDER_TAG_NAME, runContext.getProvider())
                .tag(TYPE_TAG_NAME, COMPUTATION_TYPE)
                .tag(STATUS_TAG_NAME, result != null && result.isOk() ? "OK" : "NOK")
                .register(meterRegistry)
                .increment();
    }
}
