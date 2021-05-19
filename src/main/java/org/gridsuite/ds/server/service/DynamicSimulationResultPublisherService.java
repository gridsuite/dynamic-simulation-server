/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationResultPublisherService {

    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationResultPublisherService.class.getName()
            + ".output-broker-messages";

    private final Sinks.Many<Message<String>> resultMessagePublisher = Sinks.many().multicast().onBackpressureBuffer();

    @Bean
    public Supplier<Flux<Message<String>>> publishResult() {
        return () -> resultMessagePublisher.asFlux().log(CATEGORY_BROKER_OUTPUT, Level.FINE);
    }

    public void publish(UUID resultUuid) {
        while (resultMessagePublisher.tryEmitNext(MessageBuilder
                .withPayload("")
                .setHeader("resultUuid", resultUuid.toString())
                .build()).isFailure()) {
            LockSupport.parkNanos(10);
        }
    }
}
