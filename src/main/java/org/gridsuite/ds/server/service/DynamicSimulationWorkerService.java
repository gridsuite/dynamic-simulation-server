/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.DynamicModelsSupplier;
import com.powsybl.dynamicsimulation.DynamicSimulation;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.groovy.DynamicModelGroovyExtension;
import com.powsybl.dynamicsimulation.groovy.GroovyDynamicModelsSupplier;
import com.powsybl.dynamicsimulation.groovy.GroovyExtension;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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

    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationService.class.getName() + ".output-broker-messages";

    private static final Logger OUTPUT_MESSAGE_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    private final ResultRepository resultRepository;

    private final NetworkStoreService networkStoreService;

    private FileSystem fileSystem = FileSystems.getDefault();

    @Autowired
    private StreamBridge publishResult;

    /* lazy because test use spybean on self injecting bean (DynamicSimulationWorkerService)
        without lazy springs fails with
        org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name
            'dynamicSimulationWorkerService': Bean with name 'dynamicSimulationWorkerService' has been injected into other
            beans [dynamicSimulationWorkerService] in its raw version as part of a circular reference, but has eventually
            been wrapped. This means that said other beans do not use the final version of the bean. This is often the
            result of over-eager type matching - consider using 'getBeanNamesForType'
            with the 'allowEagerInit' flag turned off, for example.

        Lazy is not a perfect solution because self is still vanilla  DynamicSimulationWorkerService, and not the mocked bean
            however it works for us now because we only mock function not called on self
        if this causes problems in the future we will need to find a better fix */
    @Lazy
    @Autowired
    DynamicSimulationWorkerService self;

    public DynamicSimulationWorkerService(NetworkStoreService networkStoreService,
                                          ResultRepository resultRepository) {
        this.networkStoreService = networkStoreService;
        this.resultRepository = resultRepository;
    }

    public Mono<DynamicSimulationResult> run(DynamicSimulationRunContext context) {
        Objects.requireNonNull(context);
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},", context.getNetworkUuid(), context.getStartTime(), context.getStopTime());

        Network network = getNetwork(context.getNetworkUuid());
        List<DynamicModelGroovyExtension> extensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynaWaltzProvider.NAME);
        GroovyDynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(new ByteArrayInputStream(context.getDynamicModelContent()), extensions);
        DynamicSimulationParameters parameters = new DynamicSimulationParameters(context.getStartTime(), context.getStopTime());
        return Mono.fromCompletionStage(runAsync(network, dynamicModelsSupplier, parameters));
    }

    public CompletableFuture<DynamicSimulationResult> runAsync(Network network,
                                                               DynamicModelsSupplier dynamicModelsSupplier,
                                                               DynamicSimulationParameters dynamicSimulationParameters) {
        return DynamicSimulation.runAsync(network, dynamicModelsSupplier, dynamicSimulationParameters);
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
                            .flatMap(result -> updateResult(resultContext.getResultUuid(), result.isOk()))
                            .doOnSuccess(unused -> {
                                Message<String> sendMessage = MessageBuilder
                                        .withPayload("")
                                        .setHeader("resultUuid", resultContext.getResultUuid().toString())
                                        .build();
                                sendResultMessage(sendMessage);
                                LOGGER.info("Dynamic simulation complete (resultUuid='{}')", resultContext.getResultUuid());
                            });
                })
                .doOnError(throwable -> LOGGER.error(throwable.toString(), throwable))
                .subscribe();
    }

    public Mono<Void> updateResult(UUID resultUuid, Boolean result) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromRunnable(() -> self.doUpdateResult(resultUuid, result));
    }

    @Transactional
    public void doUpdateResult(UUID resultUuid, Boolean result) {
        var res = resultRepository.getOne(resultUuid);
        res.setResult(result);
        res.setStatus(DynamicSimulationStatus.COMPLETED.name());
    }

    public void setFileSystem(FileSystem fs) {
        this.fileSystem = fs;
    }

    private void sendResultMessage(Message<String> message) {
        OUTPUT_MESSAGE_LOGGER.debug("Sending message : {}", message);
        publishResult.send("publishResult-out-0", message);
    }
}

