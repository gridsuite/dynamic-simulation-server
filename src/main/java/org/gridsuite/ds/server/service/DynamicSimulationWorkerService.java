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
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.computation.service.*;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.gridsuite.ds.server.service.DynamicSimulationService.COMPUTATION_TYPE;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
public class DynamicSimulationWorkerService extends AbstractWorkerService<DynamicSimulationResult, DynamicSimulationRunContext, DynamicSimulationParametersInfos, DynamicSimulationResultService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationWorkerService.class);

    private final DynamicMappingClient dynamicMappingClient;
    private final ParametersService parametersService;

    public DynamicSimulationWorkerService(NetworkStoreService networkStoreService,
                                          NotificationService notificationService,
                                          ReportService reportService,
                                          ExecutionService executionService,
                                          DynamicSimulationObserver observer,
                                          ObjectMapper objectMapper,
                                          DynamicSimulationResultService dynamicSimulationResultService,
                                          DynamicMappingClient dynamicMappingClient,
                                          ParametersService parametersService) {
        super(networkStoreService, notificationService, reportService, dynamicSimulationResultService, executionService, observer, objectMapper);
        this.dynamicMappingClient = Objects.requireNonNull(dynamicMappingClient);
        this.parametersService = Objects.requireNonNull(parametersService);
    }

    /**
     * Use this method to mock with DockerLocalComputationManager in case of integration tests with test container
     *
     * @return a computation manager
     */
    public ComputationManager getComputationManager() {
        return executionService.getComputationManager();
    }

    @Override
    protected DynamicSimulationResultContext fromMessage(Message<String> message) {
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

        DynamicSimulationStatus status = result.getStatus() == DynamicSimulationResult.Status.SUCCESS ?
                DynamicSimulationStatus.CONVERGED :
                DynamicSimulationStatus.DIVERGED;

        resultService.updateResult(resultUuid, timeSeries, timeLineSeries, status);
    }

    @Override
    protected void saveResult(Network network, AbstractResultContext<DynamicSimulationRunContext> resultContext, DynamicSimulationResult result) {
        updateResult(resultContext.getResultUuid(), result);
    }

    @Override
    protected String getComputationType() {
        return COMPUTATION_TYPE;
    }

    // open the visibility from protected to public to mock in a test where the stop arrives early
    @Override
    public void preRun(DynamicSimulationRunContext runContext) {
        super.preRun(runContext);
        DynamicSimulationParametersInfos parametersInfos = runContext.getParameters();

        // get script and parameters file from dynamic mapping server
        Script scriptObj = dynamicMappingClient.createFromMapping(runContext.getMapping());

        // get all dynamic simulation parameters
        String parametersFile = scriptObj.getParametersFile();
        DynamicSimulationParameters parameters = parametersService.getDynamicSimulationParameters(
                parametersFile.getBytes(StandardCharsets.UTF_8), runContext.getProvider(), parametersInfos);

        // set start and stop times
        parameters.setStartTime(parametersInfos.getStartTime().intValue()); // TODO remove intValue() when correct startTime to double in powsybl
        parameters.setStopTime(parametersInfos.getStopTime().intValue()); // TODO remove intValue() when correct stopTime to double in powsybl

        // groovy scripts
        String dynamicModel = scriptObj.getScript();
        String eventModel = parametersService.getEventModel(parametersInfos.getEvents());
        String curveModel = parametersService.getCurveModel(parametersInfos.getCurves());

        // enrich runContext
        runContext.setDynamicSimulationParameters(parameters);
        runContext.setDynamicModelContent(dynamicModel);
        runContext.setEventModelContent(eventModel);
        runContext.setCurveContent(curveModel);
    }

    @Override
    public CompletableFuture<DynamicSimulationResult> getCompletableFuture(Network network, DynamicSimulationRunContext runContext, String provider, UUID resultUuid) {

        List<DynamicModelGroovyExtension> dynamicModelExtensions = GroovyExtension.find(DynamicModelGroovyExtension.class, DynaWaltzProvider.NAME);
        DynamicModelsSupplier dynamicModelsSupplier = new GroovyDynamicModelsSupplier(
                new ByteArrayInputStream(runContext.getDynamicModelContent().getBytes()), dynamicModelExtensions
        );

        List<EventModelGroovyExtension> eventModelExtensions = GroovyExtension.find(EventModelGroovyExtension.class, DynaWaltzProvider.NAME);
        EventModelsSupplier eventModelsSupplier = new GroovyEventModelsSupplier(
                new ByteArrayInputStream(runContext.getEventModelContent().getBytes()), eventModelExtensions
        );

        List<CurveGroovyExtension> curveExtensions = GroovyExtension.find(CurveGroovyExtension.class, DynaWaltzProvider.NAME);
        CurvesSupplier curvesSupplier = new GroovyCurvesSupplier(
                new ByteArrayInputStream(runContext.getCurveContent().getBytes()), curveExtensions
        );

        DynamicSimulationParameters parameters = runContext.getDynamicSimulationParameters();
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},",
                runContext.getNetworkUuid(), parameters.getStartTime(), parameters.getStopTime());

        DynamicSimulation.Runner runner = DynamicSimulation.find(provider);
        return runner.runAsync(network,
                dynamicModelsSupplier,
                eventModelsSupplier,
                curvesSupplier,
                runContext.getVariantId() != null ? runContext.getVariantId() : VariantManagerConstants.INITIAL_VARIANT_ID,
                getComputationManager(),
                parameters,
                runContext.getReporter());
    }

    @Bean
    @Override
    public Consumer<Message<String>> consumeRun() {
        return super.consumeRun();
    }

    @Bean
    @Override
    public Consumer<Message<String>> consumeCancel() {
        return super.consumeCancel();
    }
}
