/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.commons.PowsyblException;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynamicsimulation.groovy.*;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.ds.server.dsl.GroovyCurvesSupplier;
import org.gridsuite.ds.server.dsl.GroovyEventModelsSupplier;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
public class DynamicSimulationWorkerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationWorkerService.class);

    private static final String CATEGORY_BROKER_INPUT = DynamicSimulationWorkerService.class.getName()
            + ".input-broker-messages";

    private static final Logger LOGGER_BROKER_INPUT = LoggerFactory.getLogger(CATEGORY_BROKER_INPUT);

    private static final String CATEGORY_BROKER_OUTPUT = DynamicSimulationService.class.getName() + ".output-broker-messages";

    private static final Logger OUTPUT_MESSAGE_LOGGER = LoggerFactory.getLogger(CATEGORY_BROKER_OUTPUT);

    private final ResultRepository resultRepository;

    private final NetworkStoreService networkStoreService;

    private FileSystem fileSystem = FileSystems.getDefault();

    @Autowired
    private StreamBridge publishResult;

    private DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult;

    public DynamicSimulationWorkerService(NetworkStoreService networkStoreService,
                                          ResultRepository resultRepository,
                                          DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult) {
        this.networkStoreService = networkStoreService;
        this.resultRepository = resultRepository;
        this.dynamicSimulationWorkerUpdateResult = dynamicSimulationWorkerUpdateResult;
    }

    public Mono<DynamicSimulationResult> run(DynamicSimulationRunContext context) {
        Objects.requireNonNull(context);
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},", context.getNetworkUuid(), context.getStartTime(), context.getStopTime());

        Network network = getNetwork(context.getNetworkUuid());

        List<DynamicModelGroovyExtension> dynamicModelExtensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynaWaltzProvider.NAME);
        DynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(new ByteArrayInputStream(context.getDynamicModelContent()), dynamicModelExtensions);

        List<EventModelGroovyExtension> eventModelExtensions = GroovyExtension.find(EventModelGroovyExtension.class, DynaWaltzProvider.NAME);
        EventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(new ByteArrayInputStream(context.getDynamicModelContent()), eventModelExtensions);

        List<CurveGroovyExtension> curveExtensions = GroovyExtension.find(CurveGroovyExtension.class, DynaWaltzProvider.NAME);
        CurvesSupplier curvesSupplier = new GroovyCurvesSupplier(new ByteArrayInputStream(context.getDynamicModelContent()), curveExtensions);

        DynamicSimulationParameters parameters = context.getParameters();
        if (parameters != null) {
            parameters = new DynamicSimulationParameters(context.getStartTime(), context.getStopTime());
        }
        parameters.setStartTime(context.getStartTime());
        parameters.setStopTime(context.getStopTime());

        return Mono.fromCompletionStage(runAsync(network,
                context.getVariantId() != null ? context.getVariantId() : VariantManagerConstants.INITIAL_VARIANT_ID,
                dynamicModelsSupplier,
                eventModelsSupplier,
                curvesSupplier,
                parameters));
    }

    public CompletableFuture<DynamicSimulationResult> runAsync(Network network,
                                                               String variantId,
                                                               DynamicModelsSupplier dynamicModelsSupplier,
                                                               EventModelsSupplier eventModelsSupplier,
                                                               CurvesSupplier curvesSupplier,
                                                               DynamicSimulationParameters dynamicSimulationParameters) {
        return DynamicSimulation.runAsync(network, dynamicModelsSupplier, eventModelsSupplier, curvesSupplier, variantId, dynamicSimulationParameters);
    }

    private Network getNetwork(UUID networkUuid) {
        try {
            return networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Bean
    public Consumer<Message<String>> consumeRun() {
        return message -> {
            LOGGER_BROKER_INPUT.debug("consume {}", message);
            try {
                DynamicSimulationResultContext resultContext = DynamicSimulationResultContext.fromMessage(message);

                run(resultContext.getRunContext())
                        .flatMap(result -> updateResult(resultContext.getResultUuid(), result))
                        .doOnSuccess(unused -> {
                            Message<String> sendMessage = MessageBuilder
                                    .withPayload("")
                                    .setHeader("resultUuid", resultContext.getResultUuid().toString())
                                    .build();
                            sendResultMessage(sendMessage);
                            LOGGER.info("Dynamic simulation complete (resultUuid='{}')", resultContext.getResultUuid());
                        }).block();
            } catch (Exception e) {
                LOGGER.error("error in consumeRun", e);
            }
        };
    }

    public Mono<Void> updateResult(UUID resultUuid, DynamicSimulationResult result) {
        Objects.requireNonNull(resultUuid);
        System.out.println(result.getCurves().toString());
        return Mono.fromRunnable(() -> dynamicSimulationWorkerUpdateResult.doUpdateResult(resultUuid, result.isOk()));
    }

    public void setFileSystem(FileSystem fs) {
        this.fileSystem = fs;
    }

    private void sendResultMessage(Message<String> message) {
        OUTPUT_MESSAGE_LOGGER.debug("Sending message : {}", message);
        publishResult.send("publishResult-out-0", message);
    }
}

