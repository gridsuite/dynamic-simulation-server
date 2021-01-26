/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.repository;

import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Repository
public class DynamicSimulationRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationRepository.class);

    private final ResultRepository resultRepository;

    public DynamicSimulationRepository(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    private static ResultEntity toEntity(UUID resultUuid, Boolean result, String status) {
        return new ResultEntity(resultUuid, result, status);
    }

    public Mono<ResultEntity> insertStatus(String status) {
        return resultRepository.save(new ResultEntity(null, null, status, true));
    }

    public Mono<String> findStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid).map(ResultEntity::getStatus);
    }

    public Mono<Void> updateResult(UUID resultUuid, Boolean result) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.save(toEntity(resultUuid, result, DynamicSimulationStatus.COMPLETED.name()))
                .then();
    }

    public Mono<Boolean> findResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid).map(ResultEntity::getResult);
    }

    public Mono<Void> delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.deleteById(resultUuid).then()
                .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable));
    }

    public Mono<Void> deleteAll() {
        Mono<Void> v1 = resultRepository.deleteAll();
        return v1.then().doOnError(throwable -> LOGGER.error(throwable.toString(), throwable));
    }

}
