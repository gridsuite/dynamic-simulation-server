/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters.impl;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.exceptions.UncheckedXmlStreamException;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import com.powsybl.dynawaltz.DynaWaltzParameters;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.dynawaltz.DynaWaltzParametersInfos;
import org.gridsuite.ds.server.dto.dynawaltz.XmlSerializableParameter;
import org.gridsuite.ds.server.dto.dynawaltz.solver.SolverInfos;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersServiceImpl implements ParametersService {

    @Override
    public byte[] getEventModel() {
        try (InputStream is = getClass().getResourceAsStream(PARAMETERS_DIR + RESOURCE_PATH_DELIMETER + EVENTS_GROOVY)) {
            // read the events.groovy in the "parameters" resources
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public byte[] getCurveModel() {
        try (InputStream is = getClass().getResourceAsStream(PARAMETERS_DIR + RESOURCE_PATH_DELIMETER + CURVES_GROOVY)) {
            // read the curves.groovy in the "parameters" resources
            return is.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams, String provider, DynamicSimulationParametersInfos inputParameters) {
        try {
            // prepare a tmp dir for current running simulation
            // TODO to remove when dynawaltz provider support streams for inputs
            Path configDir = PlatformConfig.defaultConfig().getConfigDir().orElseThrow();
            Path tmpPath = configDir.resolve(TMP_DIR);
            if (!Files.exists(tmpPath)) {
                Files.createDirectory(tmpPath);
            }
            Path workingDir = Files.createTempDirectory(tmpPath, WORKING_DIR_PREFIX);

            // load model par
            Files.copy(new ByteArrayInputStream(dynamicParams), workingDir.resolve(MODELS_PAR));

            // load two others files
            for (String parFileName : List.of(NETWORK_PAR, SOLVERS_PAR)) {
                try (InputStream parIs = getClass().getResourceAsStream(PARAMETERS_DIR + RESOURCE_PATH_DELIMETER + parFileName)) {
                    Files.copy(parIs, workingDir.resolve(parFileName));
                }
            }

            // load parameter file then config paths
            DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(getClass().getResourceAsStream(PARAMETERS_DIR + RESOURCE_PATH_DELIMETER + PARAMETERS_JSON));
            // TODO: Powsybl side - create an explicit dependency to DynaWaltz class and keep dynamic simulation abstraction all over this micro service
            if (DynaWaltzProvider.NAME.equals(provider)) {
                DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);
                dynaWaltzParameters.setParametersFile(workingDir.resolve(MODELS_PAR).toString());
                dynaWaltzParameters.getNetwork().setParametersFile(workingDir.resolve(NETWORK_PAR).toString());
                dynaWaltzParameters.getSolver().setParametersFile(workingDir.resolve(SOLVERS_PAR).toString());

                // override solver from input parameter
                DynaWaltzParametersInfos inputDynaWaltzParameters = (DynaWaltzParametersInfos) inputParameters.getExtensions().stream()
                        .filter(elem -> DynaWaltzParametersInfos.EXTENSION_NAME.equals(elem.getName()))
                        .findFirst().orElse(null);
                if (inputDynaWaltzParameters != null) {
                    SolverInfos inputSolver = inputDynaWaltzParameters.getSolvers().stream().filter(elem -> elem.getId().equals(inputDynaWaltzParameters.getSolverId())).findFirst().orElse(null);
                    if (inputSolver != null) {
                        dynaWaltzParameters.getSolver().setParametersId(inputSolver.getId());
                        dynaWaltzParameters.getSolver().setType(inputSolver.getType().toSolverType());

                        // TODO to remove when dynawaltz provider support streams for inputs
                        // export input solver to override default solver par file
                        Path file = workingDir.resolve(SOLVERS_PAR);
                        Files.deleteIfExists(file);
                        XmlSerializableParameter.writeParameter(file, XmlSerializableParameter.PARAMETER_SET, inputSolver);
                    }
                }
            }

            return parameters;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
    }
}
