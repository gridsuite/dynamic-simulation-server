/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.ds.server.service.contexts.ContextUtils.getNonNullHeader;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationResultContext {

    public static final String HEADER_RESULT_UUID = "resultUuid";
    public static final String HEADER_PROVIDER = "provider";
    public static final String HEADER_RECEIVER = "receiver";
    public static final String HEADER_NETWORK_UUID = "networkUuid";
    public static final String HEADER_VARIANT_ID = "variantId";
    public static final String HEADER_DYNAMIC_MODEL_CONTENT = "dynamicModelContent";
    public static final String HEADER_EVENT_MODEL_CONTENT = "eventModelContent";
    public static final String HEADER_CURVE_CONTENT = "curveContent";

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

    public static DynamicSimulationResultContext fromMessage(Message<byte[]> message) {
        Objects.requireNonNull(message);

        byte[] parametersPayload = message.getPayload();
        ByteArrayInputStream bytesIS = new ByteArrayInputStream(parametersPayload);
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(bytesIS);

        MessageHeaders headers = message.getHeaders();
        UUID resultUuid = UUID.fromString(getNonNullHeader(headers, HEADER_RESULT_UUID));
        String provider = getNonNullHeader(headers, HEADER_PROVIDER);
        String receiver = (String) headers.get(HEADER_RECEIVER);
        UUID networkUuid = UUID.fromString(getNonNullHeader(headers, HEADER_NETWORK_UUID));
        String variantId = (String) headers.get(HEADER_VARIANT_ID);
        byte[] dynamicModelContent = (byte[]) headers.get(HEADER_DYNAMIC_MODEL_CONTENT);
        byte[] eventModelContent = (byte[]) headers.get(HEADER_EVENT_MODEL_CONTENT);
        byte[] curveContent = (byte[]) headers.get(HEADER_CURVE_CONTENT);
        // decode the parameters

        DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(provider, receiver, networkUuid, variantId, dynamicModelContent, eventModelContent, curveContent, parameters);
        return new DynamicSimulationResultContext(resultUuid, runContext);
    }

    public Message<byte[]> toMessage() {
        DynamicSimulationParameters parameters = runContext.getParameters();
        ByteArrayOutputStream bytesOS = new ByteArrayOutputStream();
        JsonDynamicSimulationParameters.write(parameters, bytesOS);

        return MessageBuilder.withPayload(bytesOS.toByteArray())
                .setHeader(HEADER_RESULT_UUID, resultUuid.toString())
                .setHeader(HEADER_PROVIDER, runContext.getProvider())
                .setHeader(HEADER_RECEIVER, runContext.getReceiver())
                .setHeader(HEADER_NETWORK_UUID, runContext.getNetworkUuid().toString())
                .setHeader(HEADER_VARIANT_ID, runContext.getVariantId())
                .setHeader(HEADER_DYNAMIC_MODEL_CONTENT, runContext.getDynamicModelContent())
                .setHeader(HEADER_EVENT_MODEL_CONTENT, runContext.getEventModelContent())
                .setHeader(HEADER_CURVE_CONTENT, runContext.getCurveContent())
                .build();
    }

}
