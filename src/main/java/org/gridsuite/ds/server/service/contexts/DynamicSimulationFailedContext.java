/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.contexts;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationFailedContext {
    public static final String HEADER_RESULT_UUID = "resultUuid";
    public static final String HEADER_RECEIVER = "receiver";
    public static final String HEADER_MESSAGE = "message";
    public static final String HEADER_USER_ID = "userId";

    public static final int MSG_MAX_LENGTH = 256;

    private final UUID resultUuid;

    private final String receiver;

    private final String message;

    private final String userId;

    public DynamicSimulationFailedContext(String receiver, UUID resultUuid, String message, String userId) {
        this.receiver = receiver;
        this.userId = userId;
        this.resultUuid = Objects.requireNonNull(resultUuid);
        this.message = message;
    }

    public Message<String> toMessage() {
        return MessageBuilder.withPayload("")
                .setHeader(HEADER_RESULT_UUID, resultUuid.toString())
                .setHeader(HEADER_RECEIVER, receiver)
                .setHeader(HEADER_MESSAGE, shortenMessage(message))
                .setHeader(HEADER_USER_ID, userId)
                .build();
    }

    // prevent the message from being too long for rabbitmq
    // the beginning and ending are both kept, it should make it easier to identify
    public String shortenMessage(String msg) {
        if (msg == null) {
            return msg;
        }

        return msg.length() > MSG_MAX_LENGTH ?
                msg.substring(0, MSG_MAX_LENGTH / 2) + " ... " + msg.substring(msg.length() - MSG_MAX_LENGTH / 2, msg.length() - 1)
                : msg;
    }
}
