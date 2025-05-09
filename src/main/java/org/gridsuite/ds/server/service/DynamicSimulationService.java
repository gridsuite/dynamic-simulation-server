/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.ws.commons.computation.s3.S3Service;
import com.powsybl.ws.commons.computation.service.AbstractComputationService;
import com.powsybl.ws.commons.computation.service.NotificationService;
import com.powsybl.ws.commons.computation.service.UuidGeneratorService;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService extends AbstractComputationService<DynamicSimulationRunContext, DynamicSimulationResultService, DynamicSimulationStatus> {
    public static final String COMPUTATION_TYPE = "dynamic simulation";

    public DynamicSimulationService(
            NotificationService notificationService,
            ObjectMapper objectMapper,
            UuidGeneratorService uuidGeneratorService,
            DynamicSimulationResultService dynamicSimulationResultService,
            Optional<S3Service> s3Service,
            @Value("${dynamic-simulation.default-provider}") String defaultProvider) {
        super(notificationService, dynamicSimulationResultService, s3Service, objectMapper, uuidGeneratorService, defaultProvider);
    }

    @Override
    public UUID runAndSaveResult(DynamicSimulationRunContext runContext) {
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
}
