/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
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
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.RESULT_UUID_NOT_FOUND;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService {
    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationService.class.getName() + ".output-broker-messages";
    private static final Logger LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);
    public static final String MSG_RESULT_UUID_NOT_FOUND = "Result uuid not found: ";

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

    public UUID runAndSaveResult(String receiver, UUID networkUuid, String variantId, String mappingName, String provider, DynamicSimulationParametersInfos parametersInfos, String userId) {

        // check provider => if not found then set default provider
        String dsProvider = getProviders().stream()
                .filter(elem -> elem.equals(provider))
                .findFirst().orElse(getDefaultProvider());

        // get script and parameters file from dynamic mapping server
        Script scriptObj = dynamicMappingClient.createFromMapping(mappingName);

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

        // update status to running status
        ResultEntity resultEntity = insertStatus(DynamicSimulationStatus.RUNNING.name());

        // emit a message to launch the simulation by the worker service
        Message<byte[]> message = new DynamicSimulationResultContext(resultEntity.getId(), runContext).toMessage();
        notificationService.emitRunDynamicSimulationMessage(message);
        return resultEntity.getId();
    }

    public ResultEntity insertStatus(String status) {
        return resultRepository.save(new ResultEntity(null, null, null, status));
    }

    public List<UUID> updateStatus(List<UUID> resultUuids, String status) {
        // find result entities
        List<ResultEntity> resultEntities = resultRepository.findAllById(resultUuids);
        // set entity with new values
        resultEntities.forEach(resultEntity -> resultEntity.setStatus(status));
        // save entities into database
        return resultRepository.saveAllAndFlush(resultEntities).stream().map(ResultEntity::getId).toList();
    }

    public UUID getTimeSeriesId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .map(ResultEntity::getTimeSeriesId)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid));
    }

    public UUID getTimeLineId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .map(ResultEntity::getTimeLineId)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid));
    }

    public DynamicSimulationStatus getStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .map(ResultEntity::getStatus)
                .map(DynamicSimulationStatus::valueOf)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid));
    }

    public void deleteResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        ResultEntity resultEntity = resultRepository.findById(resultUuid).orElse(null);
        if (resultEntity == null) {
            return;
        }

        // call time series client to delete time-series and timeline
        timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId());
        timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId());
        // then delete result
        resultRepository.deleteById(resultUuid);
    }

    public void deleteResults() {
        resultRepository.deleteAll();
    }

    public void stop(String receiver, UUID resultUuid) {
        notificationService.emitCancelDynamicSimulationMessage(new DynamicSimulationCancelContext(receiver, resultUuid).toMessage());
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
