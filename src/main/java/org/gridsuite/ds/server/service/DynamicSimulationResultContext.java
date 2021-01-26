/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.commons.PowsyblException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationResultContext {

    private final UUID resultUuid;

    private final DynamicSimulationRunContext runContext;

    public DynamicSimulationResultContext(UUID resultUuid, DynamicSimulationRunContext runContext) {
        this.resultUuid = Objects.requireNonNull(resultUuid);
        this.runContext = Objects.requireNonNull(runContext);
    }

    public UUID getResultUuid() {
        return resultUuid;
    }

    public DynamicSimulationRunContext getRunContext() {
        return runContext;
    }

    private static String getNonNullHeader(MessageHeaders headers, String name) {
        String header = (String) headers.get(name);
        if (header == null) {
            throw new PowsyblException("Header '" + name + "' not found");
        }
        return header;
    }

    public static DynamicSimulationResultContext fromMessage(Message<String> message) {
        Objects.requireNonNull(message);
        MessageHeaders headers = message.getHeaders();
        UUID resultUuid = UUID.fromString(getNonNullHeader(headers, "resultUuid"));
        UUID networkUuid = UUID.fromString(getNonNullHeader(headers, "networkUuid"));
        int startTime = Integer.parseInt(getNonNullHeader(headers, "startTime"));
        int stopTIme = Integer.parseInt(getNonNullHeader(headers, "stopTime"));
        UUID dynamicModelFileName = UUID.fromString(getNonNullHeader(headers, "dynamicModelFileName"));
        DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(networkUuid, startTime, stopTIme, dynamicModelFileName);
        return new DynamicSimulationResultContext(resultUuid, runContext);
    }

    public Message<String> toMessage() {
        return MessageBuilder.withPayload("")
                .setHeader("resultUuid", resultUuid.toString())
                .setHeader("networkUuid", runContext.getNetworkUuid().toString())
                .setHeader("startTime", String.valueOf(runContext.getStartTime()))
                .setHeader("stopTime", String.valueOf(runContext.getStopTime()))
                .setHeader("dynamicModelFileName", runContext.getDynamicModelFileName().toString())
                .build();
    }

}
