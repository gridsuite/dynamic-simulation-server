/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.repository;

import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.model.ResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Repository
public interface ResultRepository extends JpaRepository<ResultEntity, UUID> {
    <T> Optional<T> findById(UUID id, Class<T> type);

    <T> List<T> findBy(Class<T> type);

    @Modifying
    @Query("UPDATE ResultEntity r SET r.status = :status WHERE r.id IN :resultUuids")
    int updateStatus(@Param("resultUuids") List<UUID> resultUuids, @Param("status") DynamicSimulationStatus status);

    @Modifying
    @Query("UPDATE ResultEntity r SET r.status = :status, r.timeSeriesId = :timeSeriesId, r.timeLineId = :timeLineId," +
           " r.outputState = :outputState, r.parameters = :parameters, r.dynamicModel = :dynamicModel" +
           " WHERE r.id = :resultUuid")
    int updateResult(@Param("resultUuid") UUID resultUuid, @Param("timeSeriesId") UUID timeSeriesId, @Param("timeLineId") UUID timeLineId,
                     @Param("status") DynamicSimulationStatus status, @Param("outputState") byte[] outputState,
                     @Param("parameters") byte[] parameters, @Param("dynamicModel") byte[] dynamicModel);
}
