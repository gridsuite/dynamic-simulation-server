package org.gridsuite.ds.server.service.timeseries;

import com.powsybl.timeseries.TimeSeries;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.UUID;

public interface TimeSeriesService {
    String DELIMITER = "/";
    String TIME_SERIES_END_POINT = "time-series";

    UUID sendTimeSeries(List<TimeSeries> timeSeriesList) throws HttpClientErrorException;
}
