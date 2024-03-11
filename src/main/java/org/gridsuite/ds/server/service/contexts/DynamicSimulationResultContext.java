/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.ds.server.computation.service.AbstractResultContext;
import org.gridsuite.ds.server.computation.utils.ReportContext;
import org.gridsuite.ds.server.service.parameters.DynamicSimulationParametersValues;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.ds.server.computation.service.NotificationService.*;
import static org.gridsuite.ds.server.computation.utils.ContextUtils.getNonNullHeader;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationResultContext extends AbstractResultContext<DynamicSimulationRunContext> {

    public static final String HEADER_DYNAMIC_MODEL_CONTENT = "dynamicModelContent";
    public static final String HEADER_EVENT_MODEL_CONTENT = "eventModelContent";
    public static final String HEADER_CURVE_CONTENT = "curveContent";

    public DynamicSimulationResultContext(UUID resultUuid, DynamicSimulationRunContext runContext) {
        super(resultUuid, runContext);
    }

    public static DynamicSimulationResultContext fromMessage(Message<String> message, ObjectMapper objectMapper) {
        Objects.requireNonNull(message);

        // decode the parameters values
        DynamicSimulationParametersValues parametersValues;
        try {
            parametersValues = objectMapper.treeToValue(objectMapper.readTree(message.getPayload()).get(MESSAGE_ROOT_NAME), DynamicSimulationParametersValues.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        MessageHeaders headers = message.getHeaders();
        UUID resultUuid = UUID.fromString(getNonNullHeader(headers, RESULT_UUID_HEADER));
        String provider = getNonNullHeader(headers, HEADER_PROVIDER);
        String receiver = (String) headers.get(HEADER_RECEIVER);
        UUID networkUuid = UUID.fromString(getNonNullHeader(headers, NETWORK_UUID_HEADER));
        String variantId = (String) headers.get(VARIANT_ID_HEADER);
        String reportUuidStr = (String) headers.get(REPORT_UUID_HEADER);
        UUID reportUuid = reportUuidStr != null ? UUID.fromString(reportUuidStr) : null;
        String reporterId = (String) headers.get(REPORTER_ID_HEADER);
        String reportType = (String) headers.get(REPORT_TYPE_HEADER);
        String userId = (String) headers.get(HEADER_USER_ID);

        DynamicSimulationRunContext runContext = DynamicSimulationRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .provider(provider)
                .reportContext(ReportContext.builder().reportId(reportUuid).reportName(reporterId).reportType(reportType).build())
                .userId(userId)
                .parameters(parametersValues)
                .build();

        // specific headers for dynamic simulation
        byte[] dynamicModelContent = (byte[]) headers.get(HEADER_DYNAMIC_MODEL_CONTENT);
        byte[] eventModelContent = (byte[]) headers.get(HEADER_EVENT_MODEL_CONTENT);
        byte[] curveContent = (byte[]) headers.get(HEADER_CURVE_CONTENT);
        runContext.setDynamicModelContent(dynamicModelContent);
        runContext.setEventModelContent(eventModelContent);
        runContext.setCurveContent(curveContent);

        return new DynamicSimulationResultContext(resultUuid, runContext);
    }

    @Override
    public Message<String> toMessage(ObjectMapper objectMapper) {
        return MessageBuilder.fromMessage(super.toMessage(objectMapper))
                // specific headers for dynamic simulation
                .setHeader(HEADER_DYNAMIC_MODEL_CONTENT, runContext.getDynamicModelContent())
                .setHeader(HEADER_EVENT_MODEL_CONTENT, runContext.getEventModelContent())
                .setHeader(HEADER_CURVE_CONTENT, runContext.getCurveContent())
                .build();
    }
}
