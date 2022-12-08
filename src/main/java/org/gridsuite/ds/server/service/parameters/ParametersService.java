package org.gridsuite.ds.server.service.parameters;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import com.powsybl.dynawaltz.DynaWaltzParameters;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class ParametersService {
    public static final String TMP_DIR = "/tmp";
    public static final String BASE_WORKING_DIR = "dynamic_simulation_";
    public static final String PARAMETERS_DIR = "/parameters";
    public static final String EVENTS_GROOVY = "events.groovy";
    public static final String CURVES_GROOVY = "curves.groovy";
    public static final String MODELS_PAR = "models.par";
    public static final String NETWORK_PAR = "network.par";
    public static final String SOLVERS_PAR = "solvers.par";
    public static final String PARAMETERS_JSON = "parameters.json";

    public byte[] getEventModel() throws IOException {
        // read the events.groovy in the "parameters" resources
        return getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, EVENTS_GROOVY).toString()).readAllBytes();
    }

    public byte[] getCurveModel() throws IOException {
        // read the curves.groovy in the "parameters" resources
        return getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, CURVES_GROOVY).toString()).readAllBytes();
    }

    public DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams) throws IOException {

        Path configDir = PlatformConfig.defaultConfig().getConfigDir().orElseThrow();

        // prepare a temp dir for current running simulation
        Path tmpDir = Files.createDirectories(configDir.getFileSystem().getPath(TMP_DIR, BASE_WORKING_DIR + System.currentTimeMillis()).toAbsolutePath());

        // load parametersFile in a runtime tmp directory
        Files.copy(new ByteArrayInputStream(dynamicParams), tmpDir.resolve(MODELS_PAR));

        // load two others files
        for (String parFileName : List.of(NETWORK_PAR, SOLVERS_PAR)) {
            Files.copy(getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, parFileName).toString()), tmpDir.resolve(parFileName));
        }

        // load parameter file then config paths
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, PARAMETERS_JSON).toString()));
        DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);
        dynaWaltzParameters.setParametersFile(tmpDir.resolve(MODELS_PAR).toString());
        dynaWaltzParameters.getNetwork().setParametersFile(tmpDir.resolve(NETWORK_PAR).toString());
        dynaWaltzParameters.getSolver().setParametersFile(tmpDir.resolve(SOLVERS_PAR).toString());
        return parameters;
    }
}
