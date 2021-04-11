/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.repository.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

import static io.swagger.v3.oas.integration.StringOpenApiConfigurationLoader.LOGGER;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
public class DynamicSimulationService {

    @Autowired
    private NetworkStoreService networkStoreService;

    private final ResultRepository resultRepository;

    private final DynamicSimulationRunPublisherService runPublisherService;

    public DynamicSimulationService(ResultRepository resultRepository,
                                    DynamicSimulationRunPublisherService runPublisherService) {
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.runPublisherService = Objects.requireNonNull(runPublisherService);
    }

    public Mono<UUID> runAndSaveResult(UUID networkUuid, int startTime, int stopTime, FilePart dynamicModel) {

        Mono<byte[]> fileBytes;
        fileBytes = dynamicModel.content().collectList().flatMap(all -> Mono.fromCallable(() ->
                StreamUtils.copyToByteArray(new DefaultDataBufferFactory().join(all).asInputStream())));

        return fileBytes.flatMap(bytes -> {
            DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(networkUuid, startTime, stopTime, bytes);
            // update status to running status and store the dynamicModel file
            return insertStatus(DynamicSimulationStatus.RUNNING.name())
                    .flatMap(resultEntity ->
                            Mono.fromRunnable(() -> runPublisherService.publish(new DynamicSimulationResultContext(resultEntity.getId(), runContext)))
                                    .thenReturn(resultEntity.getId())
                    );
        });
    }

    public Mono<ResultEntity> insertStatus(String status) {
        return resultRepository.save(new ResultEntity(null, null, status, true));
    }

    public Mono<Boolean> getResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid).map(ResultEntity::getResult);
    }

    public Mono<String> getStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid).map(ResultEntity::getStatus);
    }

    public Mono<Void> deleteResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.deleteById(resultUuid).then()
                .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable));
    }

    public Mono<Void> deleteResults() {
        Mono<Void> v1 = resultRepository.deleteAll();
        return v1.then().doOnError(throwable -> LOGGER.error(throwable.toString(), throwable));
    }

}
