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
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.PROVIDER_NOT_FOUND;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService extends AbstractComputationService<DynamicSimulationRunContext, DynamicSimulationResultService, DynamicSimulationStatus> {
    public static final String COMPUTATION_TYPE = "dynamic simulation";
    private final DynamicMappingClient dynamicMappingClient;
    private final ParametersService parametersService;

    public DynamicSimulationService(
            NotificationService notificationService,
            ObjectMapper objectMapper,
            UuidGeneratorService uuidGeneratorService,
            DynamicMappingClient dynamicMappingClient,
            ParametersService parametersService,
            DynamicSimulationResultService dynamicSimulationResultService,
            @Value("${dynamic-simulation.default-provider}") String defaultProvider) {
        super(notificationService, dynamicSimulationResultService, objectMapper, uuidGeneratorService, defaultProvider);
        this.dynamicMappingClient = Objects.requireNonNull(dynamicMappingClient);
        this.parametersService = Objects.requireNonNull(parametersService);
    }

    @Override
    public UUID runAndSaveResult(DynamicSimulationRunContext runContext) {
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

        // insert a new result entity with running status
        UUID resultUuid = uuidGeneratorService.generate();
        resultService.insertStatus(List.of(resultUuid), DynamicSimulationStatus.RUNNING);

        // emit a message to launch the simulation by the worker service
        Message<String> message = new DynamicSimulationResultContext(resultUuid, runContext).toMessage(objectMapper);
        notificationService.sendRunMessage(message);
        return resultUuid;
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
