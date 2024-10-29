package org.gridsuite.ds.server.service;

import com.powsybl.timeseries.TimeSeries;
import com.powsybl.ws.commons.computation.service.AbstractComputationResultService;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.dto.timeseries.TimeSeriesGroupInfos;
import org.gridsuite.ds.server.model.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.RESULT_UUID_NOT_FOUND;

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

    public UUID getTimeSeriesId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeSeriesId();
    }

    public UUID getTimeLineId(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getTimeLineId();
    }

    public byte[] getOutputState(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getOutputState();
    }

    @Transactional
    public List<UUID> updateStatus(List<UUID> resultUuids, DynamicSimulationStatus status) {
        // find result entities
        List<ResultEntity> resultEntities = resultRepository.findAllById(resultUuids);
        // set entity with new values
        resultEntities.forEach(resultEntity -> resultEntity.setStatus(status));
        // save entities into database
        return resultRepository.saveAllAndFlush(resultEntities).stream().map(ResultEntity::getId).toList();
    }

    @Transactional
    public void updateResult(UUID resultUuid, List<TimeSeries<?, ?>> timeSeries, List<TimeSeries<?, ?>> timeLineSeries, DynamicSimulationStatus status, byte[] outputState) {

        // send time-series/timeline to time-series-server
        UUID timeSeriesUuid = Optional.ofNullable(timeSeriesClient.sendTimeSeries(timeSeries))
                .map(TimeSeriesGroupInfos::getId)
                .orElse(null);
        UUID timeLineUuid = Optional.ofNullable(timeSeriesClient.sendTimeSeries(timeLineSeries))
                .map(TimeSeriesGroupInfos::getId)
                .orElse(null);

        LOGGER.info("Update dynamic simulation [resultUuid={}, timeSeriesUuid={}, timeLineUuid={}, status={}",
                resultUuid, timeSeriesUuid, timeLineUuid, status);

        // update time-series/timeline uuids and result status to the db
        ResultEntity resultEntity = resultRepository.findById(resultUuid).orElseThrow();
        resultEntity.setTimeSeriesId(timeSeriesUuid);
        resultEntity.setTimeLineId(timeLineUuid);
        resultEntity.setStatus(status);
        resultEntity.setOutputState(outputState);
    }

    @Override
    @Transactional
    public void insertStatus(List<UUID> resultUuids, DynamicSimulationStatus status) {
        Objects.requireNonNull(resultUuids);
        resultRepository.saveAll(resultUuids.stream()
                .map(uuid -> new ResultEntity(uuid, null, null, status, null)).toList());
    }

    @Override
    @Transactional
    public void delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        ResultEntity resultEntity = resultRepository.findById(resultUuid).orElse(null);
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
        List<ResultEntity> resultEntities = resultRepository.findAll();

        // call time series client to delete time-series and timeline
        for (ResultEntity resultEntity : resultEntities) {
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId());
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId());
        }

        // then delete all results in local db
        resultRepository.deleteAllById(resultEntities.stream().map(ResultEntity::getId).toList());
    }

    @Override
    public DynamicSimulationStatus findStatus(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        return resultRepository.findById(resultUuid)
                .orElseThrow(() -> new DynamicSimulationException(RESULT_UUID_NOT_FOUND, MSG_RESULT_UUID_NOT_FOUND + resultUuid))
                .getStatus();
    }
}
