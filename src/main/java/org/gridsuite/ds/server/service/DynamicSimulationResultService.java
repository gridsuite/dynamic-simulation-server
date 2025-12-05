package org.gridsuite.ds.server.service;

import com.powsybl.timeseries.TimeSeries;
import org.gridsuite.computation.error.ComputationException;
import org.gridsuite.computation.service.AbstractComputationResultService;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.gridsuite.computation.error.ComputationBusinessErrorCode.RESULT_NOT_FOUND;

@Service
public class DynamicSimulationResultService extends AbstractComputationResultService<DynamicSimulationStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSimulationResultService.class);

    public static final String MSG_RESULT_UUID_NOT_FOUND = "Result uuid not found: ";

    private final ResultRepository resultRepository;
    private final TimeSeriesClient timeSeriesClient;

    public DynamicSimulationResultService(ResultRepository resultRepository, TimeSeriesClient timeSeriesClient) {
        this.resultRepository = resultRepository;
        this.timeSeriesClient = timeSeriesClient;
    }

    @Transactional(readOnly = true)
    public UUID getTimeSeriesId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid, ResultEntity.BasicFields.class)
                .orElseThrow(() -> new ComputationException(RESULT_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeSeriesId();
    }

    @Transactional(readOnly = true)
    public UUID getTimeLineId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid, ResultEntity.BasicFields.class)
                .orElseThrow(() -> new ComputationException(RESULT_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeLineId();
    }

    @Transactional(readOnly = true)
    public byte[] getOutputState(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid, ResultEntity.OutputState.class)
                .orElseThrow(() -> new ComputationException(RESULT_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getOutputState();
    }

    @Transactional(readOnly = true)
    public byte[] getParameters(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid, ResultEntity.Parameters.class)
                .orElseThrow(() -> new ComputationException(RESULT_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getParameters();
    }

    @Transactional(readOnly = true)
    public byte[] getDynamicModel(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid, ResultEntity.DynamicModel.class)
                .orElseThrow(() -> new ComputationException(RESULT_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getDynamicModel();
    }

    @Transactional
    public List<UUID> updateStatus(List<UUID> resultUuids, DynamicSimulationStatus status) {
        return resultRepository.updateStatus(resultUuids, status) > 0 ? resultUuids : Collections.emptyList();
    }

    @Transactional
    public void updateResult(UUID resultUuid, List<TimeSeries<?, ?>> timeSeries, List<TimeSeries<?, ?>> timeLineSeries,
                             DynamicSimulationStatus status, byte[] outputState, byte[] parameters, byte[] dynamicModel) {

        // send time-series/timeline to time-series-server
        UUID timeSeriesUuid = Optional.ofNullable(timeSeriesClient.sendTimeSeries(timeSeries))
                .map(TimeSeriesGroupInfos::getId)
                .orElse(null);
        UUID timeLineUuid = Optional.ofNullable(timeSeriesClient.sendTimeSeries(timeLineSeries))
                .map(TimeSeriesGroupInfos::getId)
                .orElse(null);

        LOGGER.debug("Update dynamic simulation [resultUuid={}, timeSeriesUuid={}, timeLineUuid={}, status={}",
                resultUuid, timeSeriesUuid, timeLineUuid, status);

        // update time-series/timeline uuids, status and outputState to the db
        resultRepository.updateResult(resultUuid, timeSeriesUuid, timeLineUuid, status, outputState, parameters, dynamicModel);
    }

    @Override
    @Transactional
    public void insertStatus(List<UUID> resultUuids, DynamicSimulationStatus status) {
        Objects.requireNonNull(resultUuids);
        resultRepository.saveAll(resultUuids.stream()
                .map(uuid -> new ResultEntity(uuid, null, null, status, null, null, null, null)).toList());
    }

    @Override
    @Transactional
    public void saveDebugFileLocation(UUID resultUuid, String debugFilePath) {
        resultRepository.findById(resultUuid, ResultEntity.BasicFields.class).ifPresentOrElse(
                (var resultEntity) -> resultRepository.updateDebugFileLocation(resultUuid, debugFilePath),
                () -> resultRepository.save(new ResultEntity(resultUuid, null, null, DynamicSimulationStatus.NOT_DONE, debugFilePath, null, null, null))
        );
    }

    @Override
    @Transactional
    public void delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        ResultEntity.BasicFields resultEntity = resultRepository.findById(resultUuid, ResultEntity.BasicFields.class).orElse(null);
        if (resultEntity == null) {
            return;
        }

        // call time series client to delete time-series and timeline
        timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId());
        timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId());
        // then delete result in local db
        resultRepository.deleteById(resultUuid);
    }

    @Override
    @Transactional
    public void deleteAll() {
        List<ResultEntity.BasicFields> resultEntities = resultRepository.findBy(ResultEntity.BasicFields.class);

        // call time series client to delete time-series and timeline
        for (ResultEntity.BasicFields resultEntity : resultEntities) {
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId());
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId());
        }

        // then delete all results in local db
        resultRepository.deleteAll();
    }

    @Override
    @Transactional(readOnly = true)
    public DynamicSimulationStatus findStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid, ResultEntity.BasicFields.class)
                .map(ResultEntity.BasicFields::getStatus)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public String findDebugFileLocation(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid, ResultEntity.BasicFields.class)
                .map(ResultEntity.BasicFields::getDebugFileLocation)
                .orElse(null);
    }
}
