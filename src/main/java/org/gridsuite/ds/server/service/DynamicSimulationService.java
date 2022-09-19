/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.repository.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService {

    private final ResultRepository resultRepository;

    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationService.class.getName() + ".output-broker-messages";

    private static final Logger LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    private NotificationService notificationService;

    public DynamicSimulationService(ResultRepository resultRepository, NotificationService notificationService) {
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.notificationService = notificationService;
    }

    public Mono<UUID> runAndSaveResult(UUID networkUuid, String variantId, int startTime, int stopTime, FilePart dynamicModel) {

        Mono<byte[]> fileBytes;
        fileBytes = dynamicModel.content().collectList().flatMap(all -> Mono.fromCallable(() ->
                StreamUtils.copyToByteArray(new DefaultDataBufferFactory().join(all).asInputStream())));

        return fileBytes.flatMap(bytes -> {
            DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(networkUuid, variantId, startTime, stopTime, bytes);
            // update status to running status and store the dynamicModel file
            return insertStatus(DynamicSimulationStatus.RUNNING.name())
                    .flatMap(resultEntity ->
                            Mono.fromRunnable(() -> {
                                notificationService.emitRunMessage(resultEntity.getId().toString(), runContext);
                            })
                                    .thenReturn(resultEntity.getId())
                    );
        });
    }

    public Mono<ResultEntity> insertStatus(String status) {
        return Mono.fromCallable(() -> resultRepository.save(new ResultEntity(null, null, status)));
    }

    public Mono<Boolean> getResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromCallable(() -> resultRepository.findById(resultUuid).map(ResultEntity::getResult)
            .orElse(null));
    }

    public Mono<String> getStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromCallable(() -> resultRepository.findById(resultUuid).map(ResultEntity::getStatus).orElse(null));
    }

    public Mono<Void> deleteResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromRunnable(() -> resultRepository.deleteById(resultUuid))
            .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable)).then();
    }

    public Mono<Void> deleteResults() {
        return Mono.fromRunnable(resultRepository::deleteAll)
            .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable)).then();
    }
}
