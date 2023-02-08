/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.notification.impl;

import org.gridsuite.ds.server.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class NotificationServiceImpl implements NotificationService {
    private static final String CATEGORY_BROKER_OUTPUT = NotificationService.class.getName() + ".output-broker-messages";
    private static final Logger OUTPUT_MESSAGE_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    @Autowired
    private StreamBridge publisher;

    private void sendMessage(Message<?> message, String bindingName) {
        NotificationServiceImpl.OUTPUT_MESSAGE_LOGGER.debug("Sending message : {}", message);
        publisher.send(bindingName, message);
    }

    @Override
    public void emitRunDynamicSimulationMessage(Message<byte[]> message) {
        sendMessage(message, "publishRun-out-0");
    }

    @Override
    public void emitResultDynamicSimulationMessage(Message<String> message) {
        sendMessage(message, "publishResult-out-0");
    }

    @Override
    public void emitCancelDynamicSimulationMessage(Message<String> message) {
        sendMessage(message, "publishCancel-out-0");
    }

    @Override
    public void emitFailDynamicSimulationMessage(Message<String> message) {
        sendMessage(message, "publishFailed-out-0");
    }
}
