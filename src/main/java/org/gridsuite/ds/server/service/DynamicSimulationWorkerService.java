/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.computation.ComputationManager;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynamicsimulation.groovy.*;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationCancelContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationFailedContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.gridsuite.ds.server.service.notification.NotificationService.FAIL_MESSAGE;

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

    private final ObjectMapper objectMapper;

    private final NetworkStoreService networkStoreService;

    private final NotificationService notificationService;

    private final TimeSeriesClient timeSeriesClient;

    private final DynamicSimulationExecutionService dynamicSimulationExecutionService;

    private final DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult;

    private final DynamicSimulationObserver dynamicSimulationObserver;

    public DynamicSimulationWorkerService(ObjectMapper objectMapper,
                                          NetworkStoreService networkStoreService,
                                          NotificationService notificationService,
                                          TimeSeriesClient timeSeriesClient,
                                          DynamicSimulationExecutionService dynamicSimulationExecutionService,
                                          DynamicSimulationWorkerUpdateResult dynamicSimulationWorkerUpdateResult,
                                          DynamicSimulationObserver dynamicSimulationObserver) {
        this.objectMapper = objectMapper;
        this.networkStoreService = networkStoreService;
        this.notificationService = notificationService;
        this.timeSeriesClient = timeSeriesClient;
        this.dynamicSimulationExecutionService = dynamicSimulationExecutionService;
        this.dynamicSimulationWorkerUpdateResult = dynamicSimulationWorkerUpdateResult;
        this.dynamicSimulationObserver = dynamicSimulationObserver;
    }

    public DynamicSimulationResult run(DynamicSimulationRunContext context) throws ExecutionException, InterruptedException {
        Objects.requireNonNull(context);
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},", context.getNetworkUuid(), context.getParameters().getStartTime(), context.getParameters().getStopTime());

        Network network = getNetwork(context.getNetworkUuid());

        List<DynamicModelGroovyExtension> dynamicModelExtensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynaWaltzProvider.NAME);
        DynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(new ByteArrayInputStream(context.getDynamicModelContent()), dynamicModelExtensions);

        List<EventModelGroovyExtension> eventModelExtensions = GroovyExtension.find(EventModelGroovyExtension.class, DynaWaltzProvider.NAME);
        EventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(new ByteArrayInputStream(context.getEventModelContent()), eventModelExtensions);

        List<CurveGroovyExtension> curveExtensions = GroovyExtension.find(CurveGroovyExtension.class, DynaWaltzProvider.NAME);
        CurvesSupplier curvesSupplier = new GroovyCurvesSupplier(new ByteArrayInputStream(context.getCurveContent()), curveExtensions);

        return runAsync(network,
                context.getProvider(),
                context.getVariantId() != null ? context.getVariantId() : VariantManagerConstants.INITIAL_VARIANT_ID,
                dynamicModelsSupplier,
                eventModelsSupplier,
                curvesSupplier,
                context.getParameters()).get();
    }

    public CompletableFuture<DynamicSimulationResult> runAsync(Network network,
                                                               String provider,
                                                               String variantId,
                                                               DynamicModelsSupplier dynamicModelsSupplier,
                                                               EventModelsSupplier eventModelsSupplier,
                                                               CurvesSupplier curvesSupplier,
                                                               DynamicSimulationParameters dynamicSimulationParameters) {
        DynamicSimulation.Runner runner = DynamicSimulation.find(provider);
        return runner.runAsync(network, dynamicModelsSupplier, eventModelsSupplier, curvesSupplier, variantId, getComputationManager(), dynamicSimulationParameters);
    }

    /**
     * Use this method to mock with DockerLocalComputationManager in case of integration tests with test container
     * @return a computation manager
     */
    public ComputationManager getComputationManager() {
        return dynamicSimulationExecutionService.getComputationManager();
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
                // run simulation
                DynamicSimulationResult dynamicSimulationResult = dynamicSimulationObserver.observeRun("run",
                        resultContext.getRunContext(),
                        () -> Objects.requireNonNull(run(resultContext.getRunContext())));

                // save result
                dynamicSimulationObserver.observe("results.save",
                        resultContext.getRunContext(),
                        () -> updateResult(resultContext.getResultUuid(), dynamicSimulationResult));

                // notify result already available
                Message<String> sendMessage = MessageBuilder
                    .withPayload("")
                    .setHeader(DynamicSimulationResultContext.HEADER_RESULT_UUID, resultContext.getResultUuid().toString())
                    .setHeader(DynamicSimulationResultContext.HEADER_RECEIVER, resultContext.getRunContext().getReceiver())
                    .build();
                notificationService.emitResultDynamicSimulationMessage(sendMessage);

                LOGGER.info("Dynamic simulation complete (resultUuid='{}')", resultContext.getResultUuid());
            } catch (Exception e) {
                if (!(e instanceof CancellationException)) {
                    LOGGER.error(FAIL_MESSAGE, e);
                    // send fail notification
                    Message<String> sendMessage = new DynamicSimulationFailedContext(resultContext.getRunContext().getReceiver(),
                            resultContext.getResultUuid(),
                            FAIL_MESSAGE + " : " + e.getMessage(),
                            resultContext.getRunContext().getUserId()
                            ).toMessage();

                    notificationService.emitFailDynamicSimulationMessage(sendMessage);
                    // delete result entity in server's db
                    dynamicSimulationWorkerUpdateResult.deleteResult(resultContext.getResultUuid());
                }
            }
        };
    }

    public void updateResult(UUID resultUuid, DynamicSimulationResult result) {
        Objects.requireNonNull(resultUuid);
        List<TimeSeries<?, ?>> timeSeries = new ArrayList<>(result.getCurves().values());
        List<TimeSeries<?, ?>> timeLineSeries = new ArrayList<>();

        // collect and convert timeline event list to StringTimeSeries
        if (!CollectionUtils.isEmpty(result.getTimeLine())) {
            List<TimelineEvent> timeLines = result.getTimeLine();
            long[] timeLineIndexes = timeLines.stream().mapToLong(event -> (long) event.time()).toArray();
            String[] timeLineValues = timeLines.stream().map(event -> {
                try {
                    return objectMapper.writeValueAsString(event);
                } catch (JsonProcessingException e) {
                    throw new PowsyblException("Error while serializing time line event: " + event.toString(), e);
                }
            }).toArray(String[]::new);
            timeLineSeries.add(TimeSeries.createString("timeLine", new IrregularTimeSeriesIndex(timeLineIndexes), timeLineValues));
        }

        // send time-series/timeline to time-series-server
        UUID timeSeriesUuid = Optional.ofNullable(timeSeriesClient.sendTimeSeries(timeSeries))
                .map(TimeSeriesGroupInfos::getId)
                .orElse(null);
        UUID timeLineUuid = Optional.ofNullable(timeSeriesClient.sendTimeSeries(timeLineSeries))
                .map(TimeSeriesGroupInfos::getId)
                .orElse(null);

        // update time-series/timeline uuids and result status to the db
        DynamicSimulationStatus status = result.getStatus() == DynamicSimulationResult.Status.SUCCESS ?
                DynamicSimulationStatus.CONVERGED :
                DynamicSimulationStatus.DIVERGED;

        LOGGER.info("Update dynamic simulation [resultUuid={}, timeSeriesUuid={}, timeLineUuid={}, status={}",
                resultUuid, timeSeriesUuid, timeLineUuid, status);
        dynamicSimulationWorkerUpdateResult.doUpdateResult(resultUuid, timeSeriesUuid, timeLineUuid, status);
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
