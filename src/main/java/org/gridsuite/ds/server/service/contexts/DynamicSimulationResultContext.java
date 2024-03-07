/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import org.gridsuite.ds.server.computation.service.AbstractResultContext;
import org.gridsuite.ds.server.computation.utils.ReportContext;
import org.gridsuite.ds.server.service.parameters.DynamicSimulationParametersValues;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.ds.server.computation.service.NotificationService.HEADER_USER_ID;
import static org.gridsuite.ds.server.computation.utils.ContextUtils.getNonNullHeader;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationResultContext extends AbstractResultContext<DynamicSimulationRunContext> {
    public static final String HEADER_PROVIDER = "provider";
    public static final String HEADER_RECEIVER = "receiver";
    public static final String HEADER_DYNAMIC_MODEL_CONTENT = "dynamicModelContent";
    public static final String HEADER_EVENT_MODEL_CONTENT = "eventModelContent";
    public static final String HEADER_CURVE_CONTENT = "curveContent";

    public DynamicSimulationResultContext(UUID resultUuid, DynamicSimulationRunContext runContext) {
        super(resultUuid, runContext);
    }

    public static DynamicSimulationResultContext fromMessage(Message<?> message) {
        Objects.requireNonNull(message);

        // decode the parameters
        byte[] parametersPayload = (byte[]) message.getPayload();
        ByteArrayInputStream bytesIS = new ByteArrayInputStream(parametersPayload);
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(bytesIS);

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
        byte[] dynamicModelContent = (byte[]) headers.get(HEADER_DYNAMIC_MODEL_CONTENT);
        byte[] eventModelContent = (byte[]) headers.get(HEADER_EVENT_MODEL_CONTENT);
        byte[] curveContent = (byte[]) headers.get(HEADER_CURVE_CONTENT);

        DynamicSimulationParametersValues parametersValues = DynamicSimulationParametersValues.builder()
                .provider(provider)
                .parameters(parameters)
                .dynamicModelContent(dynamicModelContent)
                .eventModelContent(eventModelContent)
                .curveContent(curveContent)
                .build();

        DynamicSimulationRunContext runContext = DynamicSimulationRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .provider(provider)
                .reportContext(ReportContext.builder().reportId(reportUuid).reportName(reporterId).reportType(reportType).build())
                .userId(userId)
                .parameters(parametersValues)
                .build();
        return new DynamicSimulationResultContext(resultUuid, runContext);
    }

    public Message<byte[]> toMessage() {
        DynamicSimulationParametersValues parametersValues = runContext.getParameters();
        ByteArrayOutputStream bytesOS = new ByteArrayOutputStream();
        JsonDynamicSimulationParameters.write(parametersValues.parameters(), bytesOS);

        return MessageBuilder.withPayload(bytesOS.toByteArray())
                .setHeader(RESULT_UUID_HEADER, resultUuid.toString())
                .setHeader(HEADER_PROVIDER, runContext.getProvider())
                .setHeader(HEADER_RECEIVER, runContext.getReceiver())
                .setHeader(NETWORK_UUID_HEADER, runContext.getNetworkUuid().toString())
                .setHeader(VARIANT_ID_HEADER, runContext.getVariantId())
                .setHeader(REPORT_UUID_HEADER, runContext.getReportContext().getReportId() != null ?
                        runContext.getReportContext().getReportId().toString() : null)
                .setHeader(REPORTER_ID_HEADER, runContext.getReportContext().getReportName())
                .setHeader(REPORT_TYPE_HEADER, runContext.getReportContext().getReportType())
                .setHeader(HEADER_USER_ID, runContext.getUserId())
                .setHeader(HEADER_DYNAMIC_MODEL_CONTENT, parametersValues.dynamicModelContent())
                .setHeader(HEADER_EVENT_MODEL_CONTENT, parametersValues.eventModelContent())
                .setHeader(HEADER_CURVE_CONTENT, parametersValues.curveContent())
                .build();
    }

}
