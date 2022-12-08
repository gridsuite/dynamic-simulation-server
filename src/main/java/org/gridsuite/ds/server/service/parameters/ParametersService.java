package org.gridsuite.ds.server.service.parameters;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import com.powsybl.dynawaltz.DynaWaltzParameters;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Service
public class ParametersService {
    public static final String BASE_TMP_DIR = "dynamic_simulation_";
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

    private String getDynamicParameters(Path tmpDir, byte[] dynamicParams) throws IOException {
        // save dynamicParams into a temp dir then return dest path
        Path destPath = Files.write(Paths.get(tmpDir.toString(), MODELS_PAR), dynamicParams, StandardOpenOption.CREATE_NEW);
        return destPath.toString();
    }

    private String getNetworkParameters(Path tmpDir) throws IOException {
        // copy network parameter file into a temp dir then return dest path
        Path target = Paths.get(tmpDir.toString(), NETWORK_PAR);
        Files.copy(getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, NETWORK_PAR).toString()), target);
        return target.toString();
    }

    private String getSolversParameters(Path tmpDir) throws IOException {
        // copy solver parameter file into a temp dir then return dest path
        Path target = Paths.get(tmpDir.toString(), SOLVERS_PAR);
        Files.copy(getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, SOLVERS_PAR).toString()), target);
        return target.toString();
    }

    public DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams) throws IOException {
        if (dynamicParams == null) {
            return null;
        }

        // prepare a temp dir for current running simulation
        Path tmpDirPath = Files.createTempDirectory(BASE_TMP_DIR + System.currentTimeMillis());

        // load parametersFile in a runtime tmp directory
        String modelsDestPath = getDynamicParameters(tmpDirPath, dynamicParams);

        // load two others par files
        String networkDestPath = getNetworkParameters(tmpDirPath);
        String solversDestPath = getSolversParameters(tmpDirPath);

        // create a new DynamicSimulationParameters
        // load parameter file
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, PARAMETERS_JSON).toString()));
        DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);
        dynaWaltzParameters.setParametersFile(modelsDestPath);
        dynaWaltzParameters.getNetwork().setParametersFile(networkDestPath);
        dynaWaltzParameters.getSolver().setParametersFile(solversDestPath);
        return parameters;
    }
}
