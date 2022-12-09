/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import org.apache.commons.lang3.tuple.Triple;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.dynamicmapping.DynamicMappingService;
import org.gridsuite.ds.server.service.notification.NotificationService;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService {
    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationService.class.getName() + ".output-broker-messages";
    private static final Logger LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);
    private final ResultRepository resultRepository;
    private final NotificationService notificationService;
    private final DynamicMappingService dynamicMappingService;
    private final ParametersService parametersService;

    public DynamicSimulationService(ResultRepository resultRepository, NotificationService notificationService, DynamicMappingService dynamicMappingService, ParametersService parametersService) {
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.dynamicMappingService = Objects.requireNonNull(dynamicMappingService);
        this.parametersService = Objects.requireNonNull(parametersService);
    }

    public Mono<UUID> runAndSaveResult(UUID networkUuid, String variantId, int startTime, int stopTime, String mappingName) throws IOException {
        // get script and parameters file from dynamic mapping server
        Script scriptObj = dynamicMappingService.createFromMapping(mappingName);

        // get all dynamic simulation parameters
        String parametersFile = scriptObj.getParametersFile();
        DynamicSimulationParameters parameters = parametersService.getDynamicSimulationParameters(parametersFile.getBytes());

        String script = scriptObj.getScript();
        Mono< Triple<byte[], byte[], byte[]>> inputsMono = Mono.just(Triple.of(script.getBytes(StandardCharsets.UTF_8), parametersService.getEventModel(), parametersService.getCurveModel()));

        return inputsMono.flatMap(inputs -> {
            DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(networkUuid, variantId, startTime, stopTime, inputs, parameters);
            // update status to running status
            return insertStatus(DynamicSimulationStatus.RUNNING.name()).flatMap(resultEntity -> Mono.fromRunnable(() -> {
                Message<String> message = new DynamicSimulationResultContext(resultEntity.getId(), runContext).toMessage();
                notificationService.emitRunDynamicSimulationMessage(message);
            }).thenReturn(resultEntity.getId())
            );
        });
    }

    public Mono<ResultEntity> insertStatus(String status) {
        return Mono.fromCallable(() -> resultRepository.save(new ResultEntity(null, null, null, status)));
    }

    public Mono<UUID> getTimeSeriesId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromCallable(() -> resultRepository.findById(resultUuid).map(ResultEntity::getTimeSeriesId)
                .orElse(null));
    }

    public Mono<UUID> getTimeLineId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromCallable(() -> resultRepository.findById(resultUuid).map(ResultEntity::getTimeLineId)
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
