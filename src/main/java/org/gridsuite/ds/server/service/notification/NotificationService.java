package org.gridsuite.ds.server.service.notification;

import org.springframework.messaging.Message;

public interface NotificationService {
    void sendMessage(Message<String> message, String bindingName);

    void emitRunDynamicSimulationMessage(Message<String> message);

    void emitResultDynamicSimulationMessage(Message<String> message);
}
