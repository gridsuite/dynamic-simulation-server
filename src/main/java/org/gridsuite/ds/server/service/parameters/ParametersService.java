package org.gridsuite.ds.server.service.parameters;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;

import java.io.IOException;

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
