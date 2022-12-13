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
import com.powsybl.timeseries.StringTimeSeries;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.dynamicsimulation.groovy.GroovyCurvesSupplier;
import com.powsybl.dynamicsimulation.groovy.GroovyEventModelsSupplier;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import com.powsybl.dynamicsimulation.json.DynamicSimulationResultSerializer;
import org.gridsuite.ds.server.service.notification.NotificationService;
import org.gridsuite.ds.server.service.timeseries.TimeSeriesService;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

    private final TimeSeriesService timeSeriesService;

    private final DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult;

    public DynamicSimulationWorkerService(NetworkStoreService networkStoreService,
                                          NotificationService notificationService,
                                          TimeSeriesService timeSeriesService,
                                          DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult) {
        this.networkStoreService = networkStoreService;
        this.notificationService = notificationService;
        this.timeSeriesService = timeSeriesService;
        this.dynamicSimulationWorkerUpdateResult = dynamicSimulationWorkerUpdateResult;
    }

    public Mono<DynamicSimulationResult> run(DynamicSimulationRunContext context) {
        Objects.requireNonNull(context);
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},", context.getNetworkUuid(), context.getStartTime(), context.getStopTime());

        Network network = getNetwork(context.getNetworkUuid());

        List<DynamicModelGroovyExtension> dynamicModelExtensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynaWaltzProvider.NAME);
        DynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(new ByteArrayInputStream(context.getDynamicModelContent()), dynamicModelExtensions);

        List<EventModelGroovyExtension> eventModelExtensions = GroovyExtension.find(EventModelGroovyExtension.class, DynaWaltzProvider.NAME);
        EventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(new ByteArrayInputStream(context.getEventModelContent()), eventModelExtensions);

        List<CurveGroovyExtension> curveExtensions = GroovyExtension.find(CurveGroovyExtension.class, DynaWaltzProvider.NAME);
        CurvesSupplier curvesSupplier = new GroovyCurvesSupplier(new ByteArrayInputStream(context.getCurveContent()), curveExtensions);

        DynamicSimulationParameters parameters = context.getParameters();
        if (parameters == null) {
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
            DynamicSimulationResultContext resultContext = DynamicSimulationResultContext.fromMessage(message);
            try {
                run(resultContext.getRunContext())
                        .flatMap(result -> updateResult(resultContext.getResultUuid(), result))
                        .doOnSuccess(result -> {
                            // build json payload
                            ByteArrayOutputStream bytesOS = new ByteArrayOutputStream();
                            DynamicSimulationResultSerializer.write(result, bytesOS);
                            Message<String> sendMessage = MessageBuilder
                                    .withPayload(bytesOS.toString())
                                    .setHeader("resultUuid", resultContext.getResultUuid().toString())
                                    .build();
                            notificationService.emitResultDynamicSimulationMessage(sendMessage);
                            LOGGER.info("Dynamic simulation complete (resultUuid='{}')", resultContext.getResultUuid());
                        }).subscribe();
            } catch (Exception e) {
                dynamicSimulationWorkerUpdateResult.doUpdateResult(resultContext.getResultUuid(), null, null, DynamicSimulationStatus.NOT_DONE);
                LOGGER.error("error in consumeRun", e);
            }
        };
    }

    public Mono<DynamicSimulationResult> updateResult(UUID resultUuid, DynamicSimulationResult result) {
        Objects.requireNonNull(resultUuid);
        return Mono.fromRunnable(() -> {
            // send timeseries and timeline to time-series-server
            List<TimeSeries> timeSeries = new ArrayList(result.getCurves().values());
            UUID timeSeriesUuid = timeSeriesService.sendTimeSeries(timeSeries).block();
            StringTimeSeries timeLine = result.getTimeLine();
            UUID timeLineUuid = timeSeriesService.sendTimeSeries(Arrays.asList(timeLine)).block();

            dynamicSimulationWorkerUpdateResult.doUpdateResult(resultUuid, timeSeriesUuid, timeLineUuid, result.isOk() ? DynamicSimulationStatus.CONVERGED : DynamicSimulationStatus.DIVERGED);
        }).then(Mono.just(result));
    }
}

