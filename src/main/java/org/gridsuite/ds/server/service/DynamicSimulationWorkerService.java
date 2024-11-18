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
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.DUMP_FILE_ERROR;
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

    public void updateResult(UUID resultUuid, DynamicSimulationResult result, byte[] outputState) {
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

        resultService.updateResult(resultUuid, timeSeries, timeLineSeries, status, outputState);
    }

    @Override
    protected void saveResult(Network network, AbstractResultContext<DynamicSimulationRunContext> resultContext, DynamicSimulationResult result) {
        // read dump file
        Path dumpDir = getDumpDir(resultContext.getRunContext().getDynamicSimulationParameters());
        byte[] outputState = null;
        if (dumpDir != null) {
            outputState = zipDumpFile(dumpDir);
        }

        updateResult(resultContext.getResultUuid(), result, outputState);
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
        DynamicSimulationParameters parameters = parametersService.getDynamicSimulationParameters(
                parameterFileContent.getBytes(StandardCharsets.UTF_8), runContext.getProvider(), parametersInfos);

        // get mapping then generate dynamic model configs
        InputMapping inputMapping = dynamicMappingClient.getMapping(runContext.getMapping());
        List<DynamicModelConfig> dynamicModel = parametersService.getDynamicModel(inputMapping, runContext.getNetwork());
        List<EventModelConfig > eventModel = parametersService.getEventModel(parametersInfos.getEvents());

        // set start and stop times
        parameters.setStartTime(parametersInfos.getStartTime().intValue()); // TODO remove intValue() when correct startTime to double in powsybl
        parameters.setStopTime(parametersInfos.getStopTime().intValue()); // TODO remove intValue() when correct stopTime to double in powsybl

        // groovy scripts
        String curveModel = parametersService.getCurveModel(parametersInfos.getCurves());

        // enrich runContext
        runContext.setDynamicSimulationParameters(parameters);
        runContext.setDynamicModelContent(dynamicModel);
        runContext.setEventModelContent(eventModel);
        runContext.setCurveContent(curveModel);

        // create a working folder for this run
        Path workDir;
        workDir = createWorkingDirectory();
        runContext.setWorkDir(workDir);

        // enrich dump parameters
        setupDumpParameters(workDir, parameters);
    }

    @Override
    public CompletableFuture<DynamicSimulationResult> getCompletableFuture(DynamicSimulationRunContext runContext, String provider, UUID resultUuid) {

        DynamicModelsSupplier dynamicModelsSupplier = new DynawoModelsSupplier(runContext.getDynamicModelContent());

        EventModelsSupplier eventModelsSupplier = new DynawoEventModelsSupplier(runContext.getEventModelContent());

        GroovyOutputVariablesSupplier outputVariablesSupplier = new GroovyOutputVariablesSupplier(
            new ByteArrayInputStream(runContext.getCurveContent().getBytes()),
            GroovyExtension.find(OutputVariableGroovyExtension.class, DynawoSimulationProvider.NAME));

        DynamicSimulationParameters parameters = runContext.getDynamicSimulationParameters();
        LOGGER.info("Run dynamic simulation on network {}, startTime {}, stopTime {},",
                runContext.getNetworkUuid(), parameters.getStartTime(), parameters.getStopTime());

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
        removeWorkingDirectory(workDir);
    }

    // --- Dump file related methods --- //

    private void setupDumpParameters(Path workDir, DynamicSimulationParameters parameters) {
        Path dumpDir = workDir.resolve("dump");
        FileUtil.createDirectory(dumpDir);
        DynawoSimulationParameters dynawoSimulationParameters = parameters.getExtension(DynawoSimulationParameters.class);
        dynawoSimulationParameters.setDumpFileParameters(DumpFileParameters.createExportDumpFileParameters(dumpDir));
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

    private Path createWorkingDirectory() {
        Path workDir;
        Path localDir = getComputationManager().getLocalDir();
        try {
            workDir = Files.createTempDirectory(localDir, "dynamic_simulation_");
        } catch (IOException e) {
            throw new DynamicSimulationException(DUMP_FILE_ERROR, String.format("Error occurred while creating a working directory inside the local directory %s",
                    localDir.toAbsolutePath()));
        }
        return workDir;
    }

    private void removeWorkingDirectory(Path workDir) {
        if (workDir != null) {
            try {
                FileUtil.removeDir(workDir);
            } catch (IOException e) {
                LOGGER.error(String.format("%s: Error occurred while cleaning working directory at %s", getComputationType(), workDir.toAbsolutePath()), e);
            }
        } else {
            LOGGER.info("{}: No working directory to clean", getComputationType());
        }
    }
}
