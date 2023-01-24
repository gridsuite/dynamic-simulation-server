/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.contexts;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.ds.server.service.contexts.ContextUtils.getNonNullHeader;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationCancelContext {
    public static final String RESULT_UUID = "resultUuid";
    public static final String RECEIVER = "receiver";

    private final UUID resultUuid;

    private final String receiver;

    public DynamicSimulationCancelContext(String receiver, UUID resultUuid) {
        this.receiver = Objects.requireNonNull(receiver);
        this.resultUuid = Objects.requireNonNull(resultUuid);
    }

    public UUID getResultUuid() {
        return resultUuid;
    }

    public String getReceiver() {
        return receiver;
    }

    public static DynamicSimulationCancelContext fromMessage(Message<String> message) {
        Objects.requireNonNull(message);
        MessageHeaders headers = message.getHeaders();
        UUID resultUuid = UUID.fromString(getNonNullHeader(headers, RESULT_UUID));
        String receiver = (String) headers.get(RECEIVER);
        return new DynamicSimulationCancelContext(receiver, resultUuid);
    }

    public Message<String> toMessage() {
        return MessageBuilder.withPayload("")
                .setHeader(RESULT_UUID, resultUuid.toString())
                .setHeader(RECEIVER, receiver)
                .build();
    }
}
