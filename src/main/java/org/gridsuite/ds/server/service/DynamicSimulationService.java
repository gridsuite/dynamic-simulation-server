/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.computation.service.AbstractComputationService;
import org.gridsuite.computation.service.NotificationService;
import org.gridsuite.computation.service.UuidGeneratorService;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationService extends AbstractComputationService<DynamicSimulationRunContext, DynamicSimulationResultService, DynamicSimulationStatus> {
    public static final String COMPUTATION_TYPE = "dynamic simulation";

    private final ParametersService parametersService;
    private final DynamicMappingClient dynamicMappingClient;
    private final NetworkStoreService networkStoreService;

    public DynamicSimulationService(
            NotificationService notificationService,
            ObjectMapper objectMapper,
            UuidGeneratorService uuidGeneratorService,
            DynamicSimulationResultService dynamicSimulationResultService,
            @Value("${dynamic-simulation.default-provider}") String defaultProvider,
            ParametersService parametersService,
            DynamicMappingClient dynamicMappingClient,
            NetworkStoreService networkStoreService) {
        super(notificationService, dynamicSimulationResultService, objectMapper, uuidGeneratorService, defaultProvider);
        this.parametersService = parametersService;
        this.dynamicMappingClient = dynamicMappingClient;
        this.networkStoreService = networkStoreService;
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

    public List<DynamicModelConfig> exportDynamicModel(UUID networkUuid, String variantId, String mappingName) {

        InputMapping inputMapping = dynamicMappingClient.getMapping(mappingName);
        Network network = networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
        String variant = StringUtils.isBlank(variantId) ? VariantManagerConstants.INITIAL_VARIANT_ID : variantId;
        network.getVariantManager().setWorkingVariant(variant);

        return parametersService.getDynamicModel(inputMapping, network);
    }
}
