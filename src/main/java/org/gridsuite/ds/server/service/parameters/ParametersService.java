/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.event.EventInfos;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface ParametersService {

    byte[] getEventModel(List<EventInfos> events);

    byte[] getCurveModel(List<CurveInfos> curves);

    DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams, String provider, DynamicSimulationParametersInfos inputParameters);
}
