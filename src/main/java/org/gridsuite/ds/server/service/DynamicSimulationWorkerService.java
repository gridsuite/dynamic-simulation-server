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
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.computation.ComputationManager;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynamicsimulation.groovy.*;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.computation.service.*;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.gridsuite.ds.server.service.DynamicSimulationService.COMPUTATION_TYPE;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
public class DynamicSimulationWorkerService extends AbstractWorkerService<DynamicSimulationResult, DynamicSimulationRunContext, DynamicSimulationParameters> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationWorkerService.class);

    private final TimeSeriesClient timeSeriesClient;

    private final DynamicSimulationResultService dynamicSimulationResultService;

    public DynamicSimulationWorkerService(NetworkStoreService networkStoreService,
                                          NotificationService notificationService,
                                          ReportService reportService,
                                          ExecutionService executionService,
                                          DynamicSimulationObserver observer,
                                          ObjectMapper objectMapper,
                                          TimeSeriesClient timeSeriesClient,
                                          DynamicSimulationResultService dynamicSimulationResultService) {
        super(networkStoreService, notificationService, reportService, executionService, observer, objectMapper);
        this.timeSeriesClient = timeSeriesClient;
        this.dynamicSimulationResultService = dynamicSimulationResultService;
    }

    /**
     * Use this method to mock with DockerLocalComputationManager in case of integration tests with test container
     * @return a computation manager
     */
    public ComputationManager getComputationManager() {
        return executionService.getComputationManager();
    }

    @Override
    protected AbstractResultContext<DynamicSimulationRunContext> fromMessage(Message<String> message) {
        return DynamicSimulationResultContext.fromMessage(message, objectMapper);
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
        dynamicSimulationResultService.doUpdateResult(resultUuid, timeSeriesUuid, timeLineUuid, status);
    }

    @Override
    protected void saveResult(Network network, AbstractResultContext<DynamicSimulationRunContext> resultContext, DynamicSimulationResult result) {
        updateResult(resultContext.getResultUuid(), result);
    }

    @Override
    protected String getComputationType() {
        return COMPUTATION_TYPE;
    }

    @Override
    public CompletableFuture<DynamicSimulationResult> getCompletableFuture(Network network, DynamicSimulationRunContext runContext, String provider, Reporter reporter) {

        List<DynamicModelGroovyExtension> dynamicModelExtensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynaWaltzProvider.NAME);
        DynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(new ByteArrayInputStream(runContext.getDynamicModelContent().getBytes()), dynamicModelExtensions);

        List<EventModelGroovyExtension> eventModelExtensions = GroovyExtension.find(EventModelGroovyExtension.class, DynaWaltzProvider.NAME);
        EventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(new ByteArrayInputStream(runContext.getEventModelContent().getBytes()), eventModelExtensions);

        List<CurveGroovyExtension> curveExtensions = GroovyExtension.find(CurveGroovyExtension.class, DynaWaltzProvider.NAME);
        CurvesSupplier curvesSupplier = new GroovyCurvesSupplier(new ByteArrayInputStream(runContext.getCurveContent().getBytes()), curveExtensions);

        DynamicSimulationParameters parameters = runContext.getParameters();
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},", runContext.getNetworkUuid(), parameters.getStartTime(), parameters.getStopTime());

        DynamicSimulation.Runner runner = DynamicSimulation.find(provider);
        return runner.runAsync(network, dynamicModelsSupplier, eventModelsSupplier, curvesSupplier, runContext.getVariantId() != null ? runContext.getVariantId() : VariantManagerConstants.INITIAL_VARIANT_ID,
                getComputationManager(), parameters, reporter);
    }

    @Override
    protected void deleteResult(UUID resultUuid) {
        dynamicSimulationResultService.deleteResult(resultUuid);
    }
}
