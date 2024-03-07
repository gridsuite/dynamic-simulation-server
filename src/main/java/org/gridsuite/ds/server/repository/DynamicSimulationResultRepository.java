package org.gridsuite.ds.server.repository;

import lombok.AllArgsConstructor;
import org.gridsuite.ds.server.computation.repositories.ComputationResultRepository;
import org.gridsuite.ds.server.model.ResultEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Repository
public class DynamicSimulationResultRepository implements ComputationResultRepository {

    private ResultRepository resultRepository;

    @Override
    @Transactional
    public void delete(UUID resultUuid) {
        Objects.requireNonNull(resultUuid);
        resultRepository.deleteById(resultUuid);
    }

    @Override
    @Transactional
    public void deleteAll() {
        resultRepository.deleteAll();
    }

    @Transactional
    public ResultEntity insertStatus(String status) {
        return resultRepository.save(new ResultEntity(null, null, null, status));
    }

    public List<ResultEntity>findAllById(List<UUID> resultUuids) {
        return resultRepository.findAllById(resultUuids);
    }

    public Optional<ResultEntity> findById(UUID resultUuid) {
        return resultRepository.findById(resultUuid);
    }

    @Transactional
    public List<ResultEntity> saveAllAndFlush(List<ResultEntity> resultEntities) {
        return resultRepository.saveAllAndFlush(resultEntities);
    }
}
