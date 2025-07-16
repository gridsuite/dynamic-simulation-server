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
import com.powsybl.commons.io.FileUtil;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.dynamicsimulation.*;
import com.powsybl.dynamicsimulation.groovy.GroovyExtension;
import com.powsybl.dynamicsimulation.groovy.GroovyOutputVariablesSupplier;
import com.powsybl.dynamicsimulation.groovy.OutputVariableGroovyExtension;
import com.powsybl.dynawo.DumpFileParameters;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynawoModelsSupplier;
import com.powsybl.dynawo.suppliers.events.DynawoEventModelsSupplier;
import com.powsybl.dynawo.suppliers.events.EventModelConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.timeseries.IrregularTimeSeriesIndex;
import com.powsybl.timeseries.TimeSeries;
import com.powsybl.ws.commons.computation.service.*;
import com.powsybl.ws.commons.s3.S3Service;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.ParameterFile;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationResultContext;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.gridsuite.ds.server.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.*;
import static org.gridsuite.ds.server.service.DynamicSimulationService.COMPUTATION_TYPE;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class, NotificationService.class})
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
                                          S3Service s3Service,
                                          DynamicMappingClient dynamicMappingClient,
                                          ParametersService parametersService) {
        super(networkStoreService, notificationService, reportService, dynamicSimulationResultService, s3Service, executionService, observer, objectMapper);
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

    public void updateResult(UUID resultUuid, DynamicSimulationResult result, byte[] outputState, byte[] parameters, byte[] dynamicModel) {
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
                    throw new PowsyblException("Error occurred while serializing time line event: " + event.toString(), e);
                }
            }).toArray(String[]::new);
            timeLineSeries.add(TimeSeries.createString("timeLine", new IrregularTimeSeriesIndex(timeLineIndexes), timeLineValues));
        }

        DynamicSimulationStatus status = result.getStatus() == DynamicSimulationResult.Status.SUCCESS ?
                DynamicSimulationStatus.CONVERGED :
                DynamicSimulationStatus.DIVERGED;

        resultService.updateResult(resultUuid, timeSeries, timeLineSeries, status, outputState, parameters, dynamicModel);
    }

    @Override
    protected void saveResult(Network network, AbstractResultContext<DynamicSimulationRunContext> resultContext, DynamicSimulationResult result) {
        // read dump file
        DynamicSimulationParameters t0Parameters = resultContext.getRunContext().getT0DynamicSimulationParameters();
        Path dumpDir = getDumpDir(t0Parameters);
        byte[] outputState = null;
        if (dumpDir != null) {
            outputState = zipDumpFile(dumpDir);
        }

        // serialize T1 parameters to use in dynamic security analysis
        DynamicSimulationParameters t1Parameters = resultContext.getRunContext().getT1DynamicSimulationParameters();
        byte[] zippedJsonParameters = zipParameters(t1Parameters);

        // serialize T1 dynamic model to use in dynamic security analysis
        byte[] zippedJsonDynamicModel = zipDynamicModel(resultContext.getRunContext().getT1DynamicModelContent());

        updateResult(resultContext.getResultUuid(), result, outputState, zippedJsonParameters, zippedJsonDynamicModel);
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

        // get parameters file from dynamic mapping server
        ParameterFile parameterFile = dynamicMappingClient.exportParameters(runContext.getMapping());

        // get all dynamic simulation parameters
        String parameterFileContent = parameterFile.fileContent();
        // at the moment T0 and T1 share the same parameters infos, same mapping
        DynamicSimulationParameters t0Parameters = parametersService.getDynamicSimulationParameters(
                parameterFileContent.getBytes(StandardCharsets.UTF_8), runContext.getProvider(), parametersInfos);
        DynamicSimulationParameters t1Parameters = parametersService.getDynamicSimulationParameters(
                parameterFileContent.getBytes(StandardCharsets.UTF_8), runContext.getProvider(), parametersInfos);

        // get mapping then generate dynamic model configs
        InputMapping inputMapping = dynamicMappingClient.getMapping(runContext.getMapping());
        // at the moment T0 and T1 share the same dynamic model
        List<DynamicModelConfig> t0DynamicModel = parametersService.getDynamicModel(inputMapping, runContext.getNetwork());
        List<DynamicModelConfig> t1DynamicModel = parametersService.getDynamicModel(inputMapping, runContext.getNetwork());
        List<EventModelConfig > eventModel = parametersService.getEventModel(parametersInfos.getEvents());

        // set start and stop times
        t0Parameters.setStartTime(parametersInfos.getStartTime());
        t0Parameters.setStopTime(parametersInfos.getStopTime());
        if (runContext.getDebugDir() != null) {
            t0Parameters.setDebugDir(runContext.getDebugDir().toString());
        }
        t1Parameters.setStartTime(parametersInfos.getStartTime());
        t1Parameters.setStopTime(parametersInfos.getStopTime());

        // groovy scripts
        String curveModel = parametersService.getCurveModel(parametersInfos.getCurves());

        // enrich runContext
        runContext.setT0DynamicSimulationParameters(t0Parameters);
        runContext.setT1DynamicSimulationParameters(t1Parameters);

        runContext.setT0DynamicModelContent(t0DynamicModel);
        runContext.setT1DynamicModelContent(t1DynamicModel);
        runContext.setEventModelContent(eventModel);
        runContext.setCurveContent(curveModel);

        // create a working folder for this run
        Path workDir;
        workDir = createWorkingDirectory();
        runContext.setWorkDir(workDir);

        // enrich dump parameters
        setupDumpParameters(workDir, t0Parameters);
    }

    @Override
    public CompletableFuture<DynamicSimulationResult> getCompletableFuture(DynamicSimulationRunContext runContext, String provider, UUID resultUuid) {

        DynamicModelsSupplier dynamicModelsSupplier = new DynawoModelsSupplier(runContext.getT0DynamicModelContent());

        EventModelsSupplier eventModelsSupplier = new DynawoEventModelsSupplier(runContext.getEventModelContent());

        GroovyOutputVariablesSupplier outputVariablesSupplier = new GroovyOutputVariablesSupplier(
            new ByteArrayInputStream(runContext.getCurveContent().getBytes()),
            GroovyExtension.find(OutputVariableGroovyExtension.class, DynawoSimulationProvider.NAME));

        DynamicSimulationParameters parameters = runContext.getT0DynamicSimulationParameters();
        LOGGER.info("Run dynamic simulation on network {} and variant {} with mapping {}, startTime {}, stopTime {},",
                runContext.getNetworkUuid(), runContext.getVariantId(), runContext.getMapping(), parameters.getStartTime(), parameters.getStopTime());

        DynamicSimulation.Runner runner = DynamicSimulation.find(provider);
        return runner.runAsync(runContext.getNetwork(),
                dynamicModelsSupplier,
                eventModelsSupplier,
                outputVariablesSupplier,
                runContext.getVariantId() != null ? runContext.getVariantId() : VariantManagerConstants.INITIAL_VARIANT_ID,
                getComputationManager(),
                parameters,
                runContext.getReportNode());
    }

    @Override
    protected void handleNonCancellationException(AbstractResultContext<DynamicSimulationRunContext> resultContext, Exception exception, AtomicReference<ReportNode> rootReporter) {
        super.handleNonCancellationException(resultContext, exception, rootReporter);
        // try to get report nodes at powsybl level
        List<ReportNode> computationReportNodes = Optional.ofNullable(resultContext.getRunContext().getReportNode()).map(ReportNode::getChildren).orElse(null);
        if (CollectionUtils.isNotEmpty(computationReportNodes)) { // means computing has started at powsybl level
            //  re-inject result table since it has been removed by handling exception in the super
            resultService.insertStatus(List.of(resultContext.getResultUuid()), DynamicSimulationStatus.DIVERGED);
            // continue sending report for tracing reason
            super.postRun(resultContext.getRunContext(), rootReporter, null);
        }
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

    @Override
    protected void clean(AbstractResultContext<DynamicSimulationRunContext> resultContext) {
        super.clean(resultContext);
        // clean working directory
        Path workDir = resultContext.getRunContext().getWorkDir();
        removeDirectory(workDir);
    }

    @Override
    protected void processDebug(AbstractResultContext<DynamicSimulationRunContext> resultContext) {
        // copy all content from working directory into debug directory
        DynamicSimulationRunContext runContext = resultContext.getRunContext();
        if (runContext.getWorkDir() != null && runContext.getDebugDir() != null) {
            try {
                FileUtil.copyDir(runContext.getWorkDir(), runContext.getDebugDir());
            } catch (IOException e) {
                LOGGER.error("{}: Error occurred while copying directory {} to directory {} => {}",
                        getComputationType(), runContext.getWorkDir().toAbsolutePath(), runContext.getDebugDir().toAbsolutePath(), e.getMessage());
            }
        }
        super.processDebug(resultContext);
    }

    // --- Dump file related methods --- //

    private void setupDumpParameters(Path workDir, DynamicSimulationParameters parameters) {
        DynawoSimulationParameters dynawoSimulationParameters = parameters.getExtension(DynawoSimulationParameters.class);
        dynawoSimulationParameters.setDumpFileParameters(DumpFileParameters.createExportDumpFileParameters(workDir));
    }

    @Nullable
    private Path getDumpDir(DynamicSimulationParameters dynamicSimulationParameters) {
        return Optional.ofNullable(dynamicSimulationParameters)
                .map(parameters -> parameters.getExtension(DynawoSimulationParameters.class))
                .map(dynawoSimulationParameters -> ((DynawoSimulationParameters) dynawoSimulationParameters).getDumpFileParameters().dumpFileFolder())
                .orElse(null);
    }

    @Nullable
    private byte[] zipDumpFile(Path dumpDir) {
        byte[] outputState = null;
        try (Stream<Path> files = Files.list(dumpDir)) {
            // dynawo export only one dump file
            Path dumpFile = files.findFirst().orElse(null);
            if (dumpFile != null) {
                // ZIP output state
                outputState = Utils.zip(dumpFile);
            }

        } catch (IOException e) {
            throw new DynamicSimulationException(DUMP_FILE_ERROR, String.format("Error occurred while reading the dump file in the directory %s",
                    dumpDir.toAbsolutePath()));
        }
        return outputState;
    }

    private byte[] zipParameters(DynamicSimulationParameters parameters) {
        byte[] zippedJsonParameters;
        try {
            String jsonParameters = objectMapper.writeValueAsString(parameters);
            zippedJsonParameters = Utils.zip(jsonParameters);
        } catch (IOException e) {
            throw new DynamicSimulationException(DYNAMIC_SIMULATION_PARAMETERS_ERROR, "Error occurred while zipping the dynamic simulation parameters");
        }
        return zippedJsonParameters;
    }

    private byte[] zipDynamicModel(List<DynamicModelConfig> dynamicModelContent) {
        byte[] zippedJsonDynamicModelContent;
        try {
            String jsonDynamicModelContent = objectMapper.writeValueAsString(dynamicModelContent);
            zippedJsonDynamicModelContent = Utils.zip(jsonDynamicModelContent);
        } catch (IOException e) {
            throw new DynamicSimulationException(DYNAMIC_MODEL_ERROR, "Error occurred while zipping the dynamic model");
        }
        return zippedJsonDynamicModelContent;
    }

    private Path createWorkingDirectory() {
        Path workDir;
        Path localDir = getComputationManager().getLocalDir();
        try {
            workDir = Files.createTempDirectory(localDir, buildComputationDirPrefix());
        } catch (IOException e) {
            throw new DynamicSimulationException(DUMP_FILE_ERROR, String.format("Error occurred while creating a working directory inside the local directory %s",
                    localDir.toAbsolutePath()));
        }
        return workDir;
    }

}
