/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.computation.service.AbstractComputationService;
import org.gridsuite.ds.server.computation.service.NotificationService;
import org.gridsuite.ds.server.computation.service.UuidGeneratorService;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.PROVIDER_NOT_FOUND;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.RESULT_UUID_NOT_FOUND;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService extends AbstractComputationService<DynamicSimulationRunContext> {
    public static final String COMPUTATION_TYPE = "dynamic simulation";
    public static final String MSG_RESULT_UUID_NOT_FOUND = "Result uuid not found: ";
    private final DynamicMappingClient dynamicMappingClient;
    private final TimeSeriesClient timeSeriesClient;
    private final ParametersService parametersService;
    private final ResultRepository resultRepository;

    public DynamicSimulationService(
            NotificationService notificationService,
            ResultRepository resultRepository,
            ObjectMapper objectMapper,
            UuidGeneratorService uuidGeneratorService,
            DynamicMappingClient dynamicMappingClient,
            TimeSeriesClient timeSeriesClient,
            ParametersService parametersService,
            @Value("${dynamic-simulation.default-provider}") String defaultProvider) {
        super(notificationService, objectMapper, uuidGeneratorService, defaultProvider);
        this.dynamicMappingClient = Objects.requireNonNull(dynamicMappingClient);
        this.timeSeriesClient = Objects.requireNonNull(timeSeriesClient);
        this.parametersService = Objects.requireNonNull(parametersService);
        this.resultRepository = resultRepository;
    }

    @Override
    public UUID runAndSaveResult(DynamicSimulationRunContext runContext, UUID parametersUuid) {
        throw new UnsupportedOperationException("Waiting parameters moving from study-server to dynamic-simulation-server");
    }

    public UUID runAndSaveResult(DynamicSimulationRunContext runContext, DynamicSimulationParametersInfos parametersInfos) {

        // set provider for run context
        String dsProvider = runContext.getProvider();
        if (dsProvider == null) {
            dsProvider = parametersInfos.getProvider();
        }
        if (dsProvider == null) {
            dsProvider = getDefaultProvider();
        }
        runContext.setProvider(dsProvider);

        // check provider
        String provider = getProviders().stream()
                .filter(elem -> elem.equals(runContext.getProvider()))
                .findFirst().orElseThrow(() -> new DynamicSimulationException(PROVIDER_NOT_FOUND, "Dynamic simulation provider not found: " + runContext.getProvider()));

        // get script and parameters file from dynamic mapping server
        Script scriptObj = dynamicMappingClient.createFromMapping(runContext.getMapping());

        // get all dynamic simulation parameters
        String parametersFile = scriptObj.getParametersFile();
        DynamicSimulationParameters parameters = parametersService.getDynamicSimulationParameters(parametersFile.getBytes(StandardCharsets.UTF_8), provider, parametersInfos);

        // set start and stop times
        parameters.setStartTime(parametersInfos.getStartTime().intValue()); // TODO remove intValue() when correct startTime to double in powsybl
        parameters.setStopTime(parametersInfos.getStopTime().intValue()); // TODO remove intValue() when correct stopTime to double in powsybl

        // groovy scripts
        String dynamicModel = scriptObj.getScript();
        String eventModel = parametersService.getEventModel(parametersInfos.getEvents());
        String curveModel = parametersService.getCurveModel(parametersInfos.getCurves());

        // enrich runContext
        runContext.setParameters(parameters);
        runContext.setDynamicModelContent(dynamicModel);
        runContext.setEventModelContent(eventModel);
        runContext.setCurveContent(curveModel);

        // update status to running status
        ResultEntity resultEntity = insertStatus(DynamicSimulationStatus.RUNNING.name());

        // emit a message to launch the simulation by the worker service
        Message<String> message = new DynamicSimulationResultContext(resultEntity.getId(), runContext).toMessage(objectMapper);
        notificationService.sendRunMessage(message);
        return resultEntity.getId();
    }

    public ResultEntity insertStatus(String status) {
        return resultRepository.save(new ResultEntity(null, null, null, status));
    }

    @Transactional
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
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeSeriesId();
    }

    public UUID getTimeLineId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeLineId();
    }

    public DynamicSimulationStatus getStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        String status = resultRepository.findById(resultUuid)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getStatus();

        return status == null ? null : DynamicSimulationStatus.valueOf(status);
    }

    @Transactional
    @Override
    public void deleteResult(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        ResultEntity resultEntity = resultRepository.findById(resultUuid).orElse(null);
        if (resultEntity == null) {
            return;
        }

        // call time series client to delete time-series and timeline
        timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId());
        timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId());
        // then delete result in local db
        resultRepository.deleteById(resultUuid);
    }

    @Transactional
    @Override
    public void deleteResults() {
        List<ResultEntity> resultEntities = resultRepository.findAll();

        // call time series client to delete time-series and timeline
        for (ResultEntity resultEntity : resultEntities) {
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId());
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId());
        }

        // then delete all results in local db
        resultRepository.deleteAllById(resultEntities.stream().map(ResultEntity::getId).toList());
    }

    public List<String> getProviders() {
        return DynamicSimulationProvider.findAll().stream()
                .map(DynamicSimulationProvider::getName)
                .toList();
    }

    @Override
    public String getDefaultProvider() {
        return defaultProvider;
    }
}
