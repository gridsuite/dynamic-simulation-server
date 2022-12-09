/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.notification;

import org.springframework.messaging.Message;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface NotificationService {
    void sendMessage(Message<String> message, String bindingName);

    void emitRunDynamicSimulationMessage(Message<String> message);

    void emitResultDynamicSimulationMessage(Message<String> message);
}
