/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.DynamicSimulation;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.groovy.DynamicModelGroovyExtension;
import com.powsybl.dynamicsimulation.groovy.GroovyDynamicModelsSupplier;
import com.powsybl.dynamicsimulation.groovy.GroovyExtension;
import com.powsybl.dynawo.DynawoProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.repository.DynamicSimulationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Service
public class DynamicSimulationWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationWorkerService.class);

    private static final String CATEGORY_BROKER_INPUT = DynamicSimulationWorkerService.class.getName()
            + ".input-broker-messages";

    private final DynamicSimulationRepository dynamicSimulationRepository;

    private final NetworkStoreService networkStoreService;

    private final DynamicSimulationResultPublisherService resultPublisherService;

    public DynamicSimulationWorkerService(NetworkStoreService networkStoreService,
                                          DynamicSimulationRepository dynamicSimulationRepository,
                                          DynamicSimulationResultPublisherService resultPublisherService) {
        this.networkStoreService = networkStoreService;
        this.dynamicSimulationRepository = dynamicSimulationRepository;
        this.resultPublisherService = resultPublisherService;
    }

    public Mono<DynamicSimulationResult> run(DynamicSimulationRunContext context) {
        Objects.requireNonNull(context);

        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {}",
                context.getNetworkUuid(), context.getStartTime(), context.getStopTime());

        Network network = getNetwork(context.getNetworkUuid());
        List<DynamicModelGroovyExtension> extensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynawoProvider.NAME);
        GroovyDynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(Paths.get(context.getDynamicModelFileName().toString()), extensions);
        DynamicSimulationParameters parameters = new DynamicSimulationParameters(context.getStartTime(), context.getStopTime());
        CompletableFuture<DynamicSimulationResult> result = DynamicSimulation.runAsync(network, dynamicModelsSupplier, parameters);
        return Mono.fromCompletionStage(result);

    }

    private Network getNetwork(UUID networkUuid) {
        try {
            return networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Network '" + networkUuid + "' not found");
        }
    }

    @Bean
    public Consumer<Flux<Message<String>>> consumeRun() {
        return f -> f.log(CATEGORY_BROKER_INPUT, Level.FINE)
                .flatMap(message -> {
                    DynamicSimulationResultContext resultContext = DynamicSimulationResultContext.fromMessage(message);

                    return run(resultContext.getRunContext())
                            .flatMap(result -> dynamicSimulationRepository.insertResult(resultContext.getResultUuid(), result.isOk())
                                    .then(dynamicSimulationRepository.insertStatus(resultContext.getResultUuid(), DynamicSimulationStatus.COMPLETED.name())))
                            .doOnSuccess(unused -> {
                                resultPublisherService.publish(resultContext.getResultUuid());
                                LOGGER.info("Dynamic simulation complete (resultUuid='{}')", resultContext.getResultUuid());
                                try {
                                    Files.deleteIfExists(Paths.get(resultContext.getRunContext().dynamicModelFileName.toString()));
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }

                            });
                })
                .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable))
                .subscribe();
    }
}

