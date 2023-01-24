/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationCancelContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
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
    private final DynamicMappingClient dynamicMappingClient;
    private final ParametersService parametersService;

    public DynamicSimulationService(ResultRepository resultRepository, NotificationService notificationService, DynamicMappingClient dynamicMappingClient, ParametersService parametersService) {
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.dynamicMappingClient = Objects.requireNonNull(dynamicMappingClient);
        this.parametersService = Objects.requireNonNull(parametersService);
    }

    public Mono<UUID> runAndSaveResult(String receiver, UUID networkUuid, String variantId, int startTime, int stopTime, String mappingName) {

        return dynamicMappingClient.createFromMapping(mappingName) // get script and parameters file from dynamic mapping server
                .flatMap(scriptObj -> {
                    try {
                        // get all dynamic simulation parameters
                        String parametersFile = scriptObj.getParametersFile();
                        DynamicSimulationParameters parameters = parametersService.getDynamicSimulationParameters(parametersFile.getBytes());

                        String script = scriptObj.getScript();
                        byte[] dynamicModel = script.getBytes(StandardCharsets.UTF_8);
                        byte[] eventModel = parametersService.getEventModel();
                        byte[] curveModel = parametersService.getCurveModel();

                        DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(receiver, networkUuid, variantId, startTime, stopTime, dynamicModel, eventModel, curveModel, parameters);

                        return insertStatus(DynamicSimulationStatus.RUNNING.name()) // update status to running status
                                .map(resultEntity -> {
                                    Message<String> message = new DynamicSimulationResultContext(resultEntity.getId(), runContext).toMessage();
                                    notificationService.emitRunDynamicSimulationMessage(message);
                                    return resultEntity.getId();
                                });
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                }
        );
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

    public Mono<Void> stop(String receiver, UUID resultUuid) {
        return Mono.fromRunnable(() -> notificationService.emitCancelDynamicSimulationMessage(new DynamicSimulationCancelContext(receiver, resultUuid).toMessage()));
    }
}
