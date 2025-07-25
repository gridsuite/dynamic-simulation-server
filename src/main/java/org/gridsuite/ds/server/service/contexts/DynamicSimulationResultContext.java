/*
 * Copyright (c) 2021-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.contexts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.computation.service.AbstractResultContext;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.powsybl.ws.commons.computation.service.NotificationService.*;
import static com.powsybl.ws.commons.computation.utils.MessageUtils.getNonNullHeader;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationResultContext extends AbstractResultContext<DynamicSimulationRunContext> {

    public static final String HEADER_MAPPING = "mapping";

    public DynamicSimulationResultContext(UUID resultUuid, DynamicSimulationRunContext runContext) {
        super(resultUuid, runContext);
    }

    public static DynamicSimulationResultContext fromMessage(Message<String> message, ObjectMapper objectMapper) {
        Objects.requireNonNull(message);

        // decode the parameters values
        DynamicSimulationParametersInfos parametersInfos;
        try {
            parametersInfos = objectMapper.readValue(message.getPayload(), DynamicSimulationParametersInfos.class);
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
        Boolean debug = (Boolean) headers.get(HEADER_DEBUG);

        DynamicSimulationRunContext runContext = DynamicSimulationRunContext.builder()
            .networkUuid(networkUuid)
            .variantId(variantId)
            .receiver(receiver)
            .provider(provider)
            .reportInfos(ReportInfos.builder().reportUuid(reportUuid).reporterId(reporterId).computationType(reportType).build())
            .userId(userId)
            .parameters(parametersInfos)
            .debug(debug)
            .build();

        // specific headers for dynamic simulation
        runContext.setMapping(getNonNullHeader(headers, HEADER_MAPPING));

        return new DynamicSimulationResultContext(resultUuid, runContext);
    }

    @Override
    public Map<String, String> getSpecificMsgHeaders(ObjectMapper ignoredObjectMapper) {
        return Map.of(HEADER_MAPPING, getRunContext().getMapping());
    }
}
