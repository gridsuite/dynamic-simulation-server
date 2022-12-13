/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.timeseries;

import com.powsybl.timeseries.TimeSeries;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface TimeSeriesService {
    String API_VERSION = "v1";
    String DELIMITER = "/";
    String TIME_SERIES_END_POINT = "time-series";

    Mono<UUID> sendTimeSeries(List<TimeSeries> timeSeriesList) throws HttpClientErrorException;
}
