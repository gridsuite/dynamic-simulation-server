/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;

import java.io.IOException;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface ParametersService {
    String TMP_DIR = "/tmp";
    String BASE_WORKING_DIR = "dynamic_simulation_";
    String PARAMETERS_DIR = "/parameters";
    String EVENTS_GROOVY = "events.groovy";
    String CURVES_GROOVY = "curves.groovy";
    String MODELS_PAR = "models.par";
    String NETWORK_PAR = "network.par";
    String SOLVERS_PAR = "solvers.par";
    String PARAMETERS_JSON = "parameters.json";

    byte[] getEventModel() throws IOException;

    byte[] getCurveModel() throws IOException;

    DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams) throws IOException;
}
