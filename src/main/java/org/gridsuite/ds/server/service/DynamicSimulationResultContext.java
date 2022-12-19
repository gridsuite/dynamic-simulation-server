/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationResultContext {

    public static final String RESULT_UUID = "resultUuid";
    public static final String NETWORK_UUID = "networkUuid";
    public static final String VARIANT_ID = "variantId";
    public static final String START_TIME = "startTime";
    public static final String STOP_TIME = "stopTime";
    public static final String DYNAMIC_MODEL_CONTENT = "dynamicModelContent";
    public static final String EVENT_MODEL_CONTENT = "eventModelContent";
    public static final String CURVE_CONTENT = "curveContent";
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

        String parametersPayload = message.getPayload();
        ByteArrayInputStream bytesIS = new ByteArrayInputStream(parametersPayload.getBytes());
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(bytesIS);

        MessageHeaders headers = message.getHeaders();
        UUID resultUuid = UUID.fromString(getNonNullHeader(headers, RESULT_UUID));
        UUID networkUuid = UUID.fromString(getNonNullHeader(headers, NETWORK_UUID));
        String variantId = (String) headers.get(VARIANT_ID);
        int startTime = Integer.parseInt(getNonNullHeader(headers, START_TIME));
        int stopTIme = Integer.parseInt(getNonNullHeader(headers, STOP_TIME));
        byte[] dynamicModelContent = (byte[]) headers.get(DYNAMIC_MODEL_CONTENT);
        byte[] eventModelContent = (byte[]) headers.get(EVENT_MODEL_CONTENT);
        byte[] curveContent = (byte[]) headers.get(CURVE_CONTENT);
        // decode the parameters

        DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(networkUuid, variantId, startTime, stopTIme, dynamicModelContent, eventModelContent, curveContent, parameters);
        return new DynamicSimulationResultContext(resultUuid, runContext);
    }

    public Message<String> toMessage() {
        DynamicSimulationParameters parameters = runContext.getParameters();
        ByteArrayOutputStream bytesOS = new ByteArrayOutputStream();
        JsonDynamicSimulationParameters.write(parameters, bytesOS);

        return MessageBuilder.withPayload(bytesOS.toString())
                .setHeader(RESULT_UUID, resultUuid.toString())
                .setHeader(NETWORK_UUID, runContext.getNetworkUuid().toString())
                .setHeader(VARIANT_ID, runContext.getVariantId())
                .setHeader(START_TIME, String.valueOf(runContext.getStartTime()))
                .setHeader(STOP_TIME, String.valueOf(runContext.getStopTime()))
                .setHeader(DYNAMIC_MODEL_CONTENT, runContext.getDynamicModelContent())
                .setHeader(EVENT_MODEL_CONTENT, runContext.getEventModelContent())
                .setHeader(CURVE_CONTENT, runContext.getCurveContent())
                .build();
    }

}
