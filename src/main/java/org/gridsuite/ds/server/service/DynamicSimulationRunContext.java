/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationRunContext {

    private final UUID networkUuid;

    private final int startTime;

    private final int stopTime;

    private final byte[] dynamicModelContent;

    public DynamicSimulationRunContext(UUID networkUuid, int startTime, int stopTime, byte[] dynamicModelContent) {
        this.networkUuid = Objects.requireNonNull(networkUuid);
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.dynamicModelContent = dynamicModelContent;
    }

    public UUID getNetworkUuid() {
        return networkUuid;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getStopTime() {
        return stopTime;
    }

    public byte[] getDynamicModelContent() {
        return dynamicModelContent;
    }
}

