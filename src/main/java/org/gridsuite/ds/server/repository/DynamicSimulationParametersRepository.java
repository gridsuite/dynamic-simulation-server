/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.repository;

import org.gridsuite.ds.server.entities.parameters.DynamicSimulationParametersEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Repository
public interface DynamicSimulationParametersRepository extends JpaRepository<DynamicSimulationParametersEntity, UUID> {

    @Query("SELECT params.provider FROM DynamicSimulationParametersEntity params WHERE params.id = :id")
    Optional<String> findProviderById(@Param("id") UUID id);
}
