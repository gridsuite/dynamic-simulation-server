/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.network.store.client.NetworkStoreService;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.repository.DynamicSimulationRepository;
import org.gridsuite.ds.server.repository.ResultEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
public class DynamicSimulationService {

    @Autowired
    private NetworkStoreService networkStoreService;

    private final DynamicSimulationRepository dynamicSimulationRepository;

    private final DynamicSimulationRunPublisherService runPublisherService;

    public DynamicSimulationService(DynamicSimulationRepository dynamicSimulationRepository,
                                    DynamicSimulationRunPublisherService runPublisherService) {
        this.dynamicSimulationRepository = Objects.requireNonNull(dynamicSimulationRepository);
        this.runPublisherService = Objects.requireNonNull(runPublisherService);
    }

    public Mono<UUID> runAndSaveResult(UUID networkUuid, int startTime, int stopTime, FilePart dynamicModel) {

        Mono<byte[]> fileBytes;
        fileBytes = dynamicModel.content().collectList().flatMap(all -> Mono.fromCallable(() ->
                StreamUtils.copyToByteArray(new DefaultDataBufferFactory().join(all).asInputStream())));

        return fileBytes.flatMap(bytes -> {
            String fileContent = new String(bytes, StandardCharsets.UTF_8);
            DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(networkUuid, startTime, stopTime, fileContent, UUID.randomUUID().toString());
            // update status to running status and store the dynamicModel file
            return setStatus(DynamicSimulationStatus.RUNNING.name())
                    .flatMap(resultEntity ->
                            Mono.fromRunnable(() -> runPublisherService.publish(new DynamicSimulationResultContext(resultEntity.getId(), runContext)))
                                    .thenReturn(resultEntity.getId())
                    );
        });
    }

    public Mono<String> getStatus(UUID resultUuid) {
        return dynamicSimulationRepository.findStatus(resultUuid);
    }

    public Mono<ResultEntity> setStatus(String status) {
        return dynamicSimulationRepository.insertStatus(status);
    }

    public Mono<Boolean> getResult(UUID resultUuid) {
        return dynamicSimulationRepository.findResult(resultUuid);
    }

    public Mono<Void> deleteResult(UUID resultUuid) {
        return dynamicSimulationRepository.delete(resultUuid);
    }

    public Mono<Void> deleteResults() {
        return dynamicSimulationRepository.deleteAll();
    }
}
