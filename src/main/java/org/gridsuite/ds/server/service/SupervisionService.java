/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service;

import org.gridsuite.ds.server.entities.ResultEntity;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.gridsuite.ds.server.service.client.timeseries.TimeSeriesClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
@Service
public class SupervisionService {
    private final ResultRepository resultRepository;
    private final TimeSeriesClient timeSeriesClient;

    public SupervisionService(ResultRepository resultRepository, TimeSeriesClient timeSeriesClient) {
        this.resultRepository = resultRepository;
        this.timeSeriesClient = timeSeriesClient;
    }

    public Integer getResultsCount() {
        return (int) resultRepository.count();
    }

    @Transactional
    public void deleteAllResults() {
        List<ResultEntity.BasicFields> resultEntities = resultRepository.findBy(ResultEntity.BasicFields.class);

        // call time series client to delete time-series and timeline
        for (ResultEntity.BasicFields resultEntity : resultEntities) {
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeSeriesId());
            timeSeriesClient.deleteTimeSeriesGroup(resultEntity.getTimeLineId());
        }

        // then delete all results in local db
        resultRepository.deleteAll();
    }
}
