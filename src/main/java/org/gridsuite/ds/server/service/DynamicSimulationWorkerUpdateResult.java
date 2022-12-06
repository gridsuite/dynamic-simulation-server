package org.gridsuite.ds.server.service;

import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// This class is created to avoid self circular in DynamicSimulationWorkerService class
@Service
public class DynamicSimulationWorkerUpdateResult {

    private final ResultRepository resultRepository;

    public DynamicSimulationWorkerUpdateResult(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @Transactional
    public void doUpdateResult(UUID resultUuid, UUID timeSeriesUuid, UUID timeLineUuid, DynamicSimulationStatus status) {
        var res = resultRepository.getReferenceById(resultUuid);
        res.setTimeSeriesId(timeSeriesUuid);
        res.setTimeLineId(timeLineUuid);
        res.setStatus(status.name());
    }
}
