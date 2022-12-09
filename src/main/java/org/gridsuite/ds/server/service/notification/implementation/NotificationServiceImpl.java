package org.gridsuite.ds.server.service.notification.implementation;

import org.gridsuite.ds.server.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final String CATEGORY_BROKER_OUTPUT = NotificationService.class.getName() + ".output-broker-messages";
    private static final Logger OUTPUT_MESSAGE_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    @Autowired
    private StreamBridge publisher;

    @Override public void sendMessage(Message<String> message, String bindingName) {
        NotificationServiceImpl.OUTPUT_MESSAGE_LOGGER.debug("Sending message : {}", message);
        publisher.send(bindingName, message);
    }

    @Override public void emitRunDynamicSimulationMessage(Message<String> message) {
        sendMessage(message, "publishRun-out-0");
    }

    @Override public void emitResultDynamicSimulationMessage(Message<String> message) {
        sendMessage(message, "publishResult-out-0");
    }
}
