/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationRunContext {

    private final String provider;

    private final String receiver;

    private final UUID networkUuid;

    private final String variantId;

    private final byte[] dynamicModelContent;

    private final byte[] eventModelContent;

    private final byte[] curveContent;

    private final DynamicSimulationParameters parameters;

    public DynamicSimulationRunContext(String provider, String receiver, UUID networkUuid, String variantId, byte[] dynamicModelContent, byte[] eventModelContent, byte[] curveContent, DynamicSimulationParameters parameters) {
        this.provider = provider;
        this.receiver = receiver;
        this.networkUuid = Objects.requireNonNull(networkUuid);
        this.variantId = variantId;
        this.dynamicModelContent = dynamicModelContent;
        this.eventModelContent = eventModelContent;
        this.curveContent = curveContent;
        this.parameters = parameters;
    }

    public String getProvider() {
        return provider;
    }

    public String getReceiver() {
        return receiver; }

    public UUID getNetworkUuid() {
        return networkUuid;
    }

    public String getVariantId() {
        return variantId;
    }

    public byte[] getDynamicModelContent() {
        return dynamicModelContent;
    }

    public byte[] getEventModelContent() {
        return eventModelContent;
    }

    public byte[] getCurveContent() {
        return curveContent;
    }

    public DynamicSimulationParameters getParameters() {
        return parameters;
    }
}

