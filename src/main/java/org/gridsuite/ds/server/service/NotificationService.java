/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service;

import org.gridsuite.ds.server.utils.annotations.PostCompletion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * @author Seddik Yengui <seddik.yengui at rte-france.com
 */

@Service
public class NotificationService {
    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationService.class.getName() + ".output-broker-messages";
    private static final Logger LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);
    public static final String RESULT_UUID_HEADER = "resultUuid";
    public static final String NETWORK_UUID_HEADER = "networkUuid";
    public static final String VARIANT_ID_HEADER = "variantId";
    public static final String START_TIME_HEADER = "startTime";
    public static final String STOP_TIME_HEADER = "stopTime";
    public static final String DYNAMIC_MODEL_CONTENT_HEADER = "dynamicModelContent";

    @Autowired
    private StreamBridge publisher;

    private void sendMessage(Message<String> message, String bindingName) {
        LOGGER.debug("Sending message : {}", message);
        publisher.send(bindingName, message);
    }

    @PostCompletion
    public void emitRunMessage(String resultUuid, DynamicSimulationRunContext runContext) {
        sendMessage(MessageBuilder.withPayload("")
                                  .setHeader(RESULT_UUID_HEADER, resultUuid)
                                  .setHeader(NETWORK_UUID_HEADER, runContext.getNetworkUuid().toString())
                                  .setHeader(VARIANT_ID_HEADER, runContext.getVariantId())
                                  .setHeader(START_TIME_HEADER, String.valueOf(runContext.getStartTime()))
                                  .setHeader(STOP_TIME_HEADER, String.valueOf(runContext.getStopTime()))
                                  .setHeader(DYNAMIC_MODEL_CONTENT_HEADER, runContext.getDynamicModelContent())
                                  .build(),
                "publishRun-out-0");
    }

    @PostCompletion
    public void emitResultMessage(String resultUuid) {
        sendMessage(MessageBuilder.withPayload("")
                                  .setHeader(RESULT_UUID_HEADER, resultUuid)
                                  .build(),
                "publishResult-out-0");
    }
}
