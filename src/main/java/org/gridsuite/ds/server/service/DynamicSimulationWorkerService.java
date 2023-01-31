/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.io.FileUtil;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynamicsimulation.groovy.*;
import com.powsybl.dynawaltz.DynaWaltzParameters;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.timeseries.StringTimeSeries;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.dynamicsimulation.groovy.GroovyCurvesSupplier;
import com.powsybl.dynamicsimulation.groovy.GroovyEventModelsSupplier;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationCancelContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.notification.NotificationService;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.util.*;
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

    private final NetworkStoreService networkStoreService;

    private final NotificationService notificationService;

    private final TimeSeriesClient timeSeriesClient;

    private final DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult;

    public DynamicSimulationWorkerService(NetworkStoreService networkStoreService,
                                          NotificationService notificationService,
                                          TimeSeriesClient timeSeriesClient,
                                          DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult) {
        this.networkStoreService = networkStoreService;
        this.notificationService = notificationService;
        this.timeSeriesClient = timeSeriesClient;
        this.dynamicSimulationWorkerUpdateResult = dynamicSimulationWorkerUpdateResult;
    }

    public Mono<DynamicSimulationResult> run(DynamicSimulationRunContext context) {
        Objects.requireNonNull(context);
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},", context.getNetworkUuid(), context.getParameters().getStartTime(), context.getParameters().getStopTime());

        Network network = getNetwork(context.getNetworkUuid());

        List<DynamicModelGroovyExtension> dynamicModelExtensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynaWaltzProvider.NAME);
        DynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(new ByteArrayInputStream(context.getDynamicModelContent()), dynamicModelExtensions);

        List<EventModelGroovyExtension> eventModelExtensions = GroovyExtension.find(EventModelGroovyExtension.class, DynaWaltzProvider.NAME);
        EventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(new ByteArrayInputStream(context.getEventModelContent()), eventModelExtensions);

        List<CurveGroovyExtension> curveExtensions = GroovyExtension.find(CurveGroovyExtension.class, DynaWaltzProvider.NAME);
        CurvesSupplier curvesSupplier = new GroovyCurvesSupplier(new ByteArrayInputStream(context.getCurveContent()), curveExtensions);

        return Mono.fromCompletionStage(runAsync(network,
                context.getVariantId() != null ? context.getVariantId() : VariantManagerConstants.INITIAL_VARIANT_ID,
                dynamicModelsSupplier,
                eventModelsSupplier,
                curvesSupplier,
                context.getParameters()));
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
    public Consumer<Message<byte[]>> consumeRun() {
        return message -> {
            LOGGER_BROKER_INPUT.debug("consumeRun {}", message);
            DynamicSimulationResultContext resultContext = DynamicSimulationResultContext.fromMessage(message);
            try {
                run(resultContext.getRunContext())
                        .flatMap(result -> {
                            // clean temporary directory created in the config dir by the ParametersService
                            // TODO to remove when dynawaltz provider support streams for inputs
                            DynaWaltzParameters dynaWaltzParameters = resultContext.getRunContext().getParameters().getExtension(DynaWaltzParameters.class);
                            FileSystem fs = PlatformConfig.defaultConfig().getConfigDir().orElseThrow().getFileSystem();
                            try {
                                FileUtil.removeDir(fs.getPath(dynaWaltzParameters.getParametersFile()).getParent());
                            } catch (IOException e) {
                                return Mono.error(new UncheckedIOException(e));
                            }

                            return updateResult(resultContext.getResultUuid(), result);
                        })
                        .doOnSuccess(result -> {
                            Message<String> sendMessage = MessageBuilder
                                    .withPayload("")
                                    .setHeader("resultUuid", resultContext.getResultUuid().toString())
                                    .setHeader("receiver", resultContext.getRunContext().getReceiver())
                                    .build();
                            notificationService.emitResultDynamicSimulationMessage(sendMessage);
                            LOGGER.info("Dynamic simulation complete (resultUuid='{}')", resultContext.getResultUuid());
                        })
                        .block();
            } catch (Exception e) {
                dynamicSimulationWorkerUpdateResult.doUpdateResult(resultContext.getResultUuid(), null, null, DynamicSimulationStatus.NOT_DONE);
                LOGGER.error("error in consumeRun", e);
                // S2142 Restore interrupted state...
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    public Mono<DynamicSimulationResult> updateResult(UUID resultUuid, DynamicSimulationResult result) {
        Objects.requireNonNull(resultUuid);
        List<TimeSeries> timeSeries = new ArrayList<>(result.getCurves().values());
        StringTimeSeries timeLine = result.getTimeLine();
        return Mono.zip(
                    timeSeriesClient.sendTimeSeries(timeSeries).subscribeOn(Schedulers.boundedElastic()),
                    timeSeriesClient.sendTimeSeries(Arrays.asList(timeLine)).subscribeOn(Schedulers.boundedElastic())
                )
                .map(uuidTuple -> {
                    UUID timeSeriesUuid = uuidTuple.getT1().getId();
                    UUID timeLineUuid = uuidTuple.getT2().getId();
                    DynamicSimulationStatus status = result.isOk() ? DynamicSimulationStatus.CONVERGED : DynamicSimulationStatus.DIVERGED;

                    dynamicSimulationWorkerUpdateResult.doUpdateResult(resultUuid, timeSeriesUuid, timeLineUuid, status);
                    return result;
                });
    }

    @Bean
    public Consumer<Message<String>> consumeCancel() {
        return message -> {
            LOGGER_BROKER_INPUT.debug("consumeCancel {}", message);
            DynamicSimulationCancelContext cancelContext = DynamicSimulationCancelContext.fromMessage(message);
            // TODO cancel implementation
        };
    }
}

