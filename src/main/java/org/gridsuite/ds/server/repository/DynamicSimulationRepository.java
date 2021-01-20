/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Repository
public class DynamicSimulationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationRepository.class);

    private final GlobalStatusRepository globalStatusRepository;

    private final ResultRepository resultRepository;

    public DynamicSimulationRepository(GlobalStatusRepository globalStatusRepository, ResultRepository resultRepository) {
        this.globalStatusRepository = globalStatusRepository;
        this.resultRepository = resultRepository;
    }

    private static GlobalStatusEntity toEntity(UUID resultUuid, String status) {
        return new GlobalStatusEntity(resultUuid, status);
    }

    private static ResultEntity toEntity(UUID resultUuid, Boolean result) {
        return new ResultEntity(resultUuid, result);
    }

    public Mono<Void> insertStatus(UUID resultUuid, String status) {
        Objects.requireNonNull(resultUuid);
        return globalStatusRepository.insert(toEntity(resultUuid, status))
                .then();
    }

    public Mono<String> findStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return globalStatusRepository.findByResultUuid(resultUuid).map(GlobalStatusEntity::getStatus);
    }

    public Mono<Void> insertResult(UUID resultUuid, Boolean result) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.insert(toEntity(resultUuid, result))
                .then();
    }

    public Mono<Boolean> findResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findByResultUuid(resultUuid).map(ResultEntity::getResult);
    }

    public Mono<Void> delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        Mono<Void> v1 = globalStatusRepository.deleteByResultUuid(resultUuid);
        Mono<Void> v2 = resultRepository.deleteByResultUuid(resultUuid);

        return Flux.concat(v1, v2)
                .then()
                .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable));
    }

    public Mono<Void> deleteAll() {
        Mono<Void> v1 = globalStatusRepository.deleteAll();
        Mono<Void> v2 = resultRepository.deleteAll();
        return Flux.concat(v1, v2)
                .then()
                .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable));
    }

}
