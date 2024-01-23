/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationCancelContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.notification.NotificationService;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService {
    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationService.class.getName() + ".output-broker-messages";
    private static final Logger LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);
    public static final String RESULT_UUID_NOT_FOUND = "Result uuid not found: ";

    private final String defaultProvider;
    private final ResultRepository resultRepository;
    private final NotificationService notificationService;
    private final DynamicMappingClient dynamicMappingClient;
    private final TimeSeriesClient timeSeriesClient;
    private final ParametersService parametersService;

    public DynamicSimulationService(
            @Value("${dynamic-simulation.default-provider}") String defaultProvider,
            ResultRepository resultRepository,
            NotificationService notificationService,
            DynamicMappingClient dynamicMappingClient,
            TimeSeriesClient timeSeriesClient,
            ParametersService parametersService) {
        this.defaultProvider = Objects.requireNonNull(defaultProvider);
        this.resultRepository = Objects.requireNonNull(resultRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.dynamicMappingClient = Objects.requireNonNull(dynamicMappingClient);
        this.timeSeriesClient = Objects.requireNonNull(timeSeriesClient);
        this.parametersService = Objects.requireNonNull(parametersService);
    }

    public Mono<UUID> runAndSaveResult(String receiver, UUID networkUuid, String variantId, String mappingName, String provider, DynamicSimulationParametersInfos parametersInfos, String userId) {

        // check provider => if not found then set default provider
        String dsProvider = getProviders().stream()
                .filter(elem -> elem.equals(provider))
                .findFirst().orElse(getDefaultProvider());

        return dynamicMappingClient.createFromMapping(mappingName) // get script and parameters file from dynamic mapping server
                .flatMap(scriptObj -> {
                    // get all dynamic simulation parameters
                    String parametersFile = scriptObj.getParametersFile();
                    DynamicSimulationParameters parameters = parametersService.getDynamicSimulationParameters(parametersFile.getBytes(StandardCharsets.UTF_8), dsProvider, parametersInfos);

                    // set start and stop times
                    parameters.setStartTime(parametersInfos.getStartTime().intValue()); // TODO remove intValue() when correct startTime to double in powsybl
                    parameters.setStopTime(parametersInfos.getStopTime().intValue()); // TODO remove intValue() when correct stopTime to double in powsybl

                    // groovy scripts
                    String script = scriptObj.getScript();
                    byte[] dynamicModel = script.getBytes(StandardCharsets.UTF_8);
                    byte[] eventModel = parametersService.getEventModel(parametersInfos.getEvents());
                    byte[] curveModel = parametersService.getCurveModel(parametersInfos.getCurves());

                    DynamicSimulationRunContext runContext = new DynamicSimulationRunContext(dsProvider, receiver, networkUuid, variantId, dynamicModel, eventModel, curveModel, parameters, userId);

                    return insertStatus(DynamicSimulationStatus.RUNNING.name()) // update status to running status
                            .map(resultEntity -> {
                                Message<byte[]> message = new DynamicSimulationResultContext(resultEntity.getId(), runContext).toMessage();
                                notificationService.emitRunDynamicSimulationMessage(message);
                                return resultEntity.getId();
                            });
                }
        );
    }

    public Mono<ResultEntity> insertStatus(String status) {
        return Mono.fromCallable(() -> resultRepository.save(new ResultEntity(null, null, null, status)));
    }

    public Mono<List<UUID> > updateStatus(List<UUID> resultUuids, String status) {
        return Mono.fromCallable(() -> {
            // find result entities
            List<ResultEntity> resultEntities = resultRepository.findAllById(resultUuids);
            // set entity with new values
            resultEntities.forEach(resultEntity -> resultEntity.setStatus(status));
            // save entities into database
            return resultRepository.saveAllAndFlush(resultEntities).stream().map(ResultEntity::getId).toList();
        });
    }

    public Mono<UUID> getTimeSeriesId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromCallable(() -> resultRepository.findById(resultUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeSeriesId());
    }

    public Mono<UUID> getTimeLineId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromCallable(() -> resultRepository.findById(resultUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeLineId());
    }

    public Mono<DynamicSimulationStatus> getStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromCallable(() -> resultRepository.findById(resultUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, RESULT_UUID_NOT_FOUND + resultUuid))
                .getStatus())
                .mapNotNull(DynamicSimulationStatus::valueOf);
    }

    public Mono<Void> deleteResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        ResultEntity resultEntity = resultRepository.findById(resultUuid).orElse(null);
        if (resultEntity == null) {
            return Mono.empty();
        }

        // call time series client to delete timeseries and timeline
        return timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId())
            .then(timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId()))
            .then(Mono.fromRunnable(() -> resultRepository.deleteById(resultUuid))) // then delete result
            .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable)).then();
    }

    public Mono<Void> deleteResults() {
        return Mono.fromRunnable(resultRepository::deleteAll)
                .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable)).then();
    }

    public Mono<Void> stop(String receiver, UUID resultUuid) {
        return Mono.fromRunnable(() -> notificationService.emitCancelDynamicSimulationMessage(new DynamicSimulationCancelContext(receiver, resultUuid).toMessage()));
    }

    public List<String> getProviders() {
        return DynamicSimulationProvider.findAll().stream()
                .map(DynamicSimulationProvider::getName)
                .toList();
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }
}
