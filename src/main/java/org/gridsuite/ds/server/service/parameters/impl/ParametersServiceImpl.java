/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters.impl;

import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import com.powsybl.dynawaltz.DynaWaltzParameters;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.gridsuite.ds.server.utils.xml.XmlMerge;
import org.gridsuite.ds.server.utils.xml.implementation.SimpleXmlMerge;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersServiceImpl implements ParametersService {

    @Override
    public byte[] getEventModel() throws IOException {
        // read the events.groovy in the "parameters" resources
        return getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, EVENTS_GROOVY).toString()).readAllBytes();
    }

    @Override
    public byte[] getCurveModel() throws IOException {
        // read the curves.groovy in the "parameters" resources
        return getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, CURVES_GROOVY).toString()).readAllBytes();
    }

    @Override
    public DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams) throws IOException, RuntimeException {

        // prepare a temp dir for current running simulation
        Path configDir = PlatformConfig.defaultConfig().getConfigDir().orElseThrow();
        Path workingDir = Files.createTempDirectory(configDir, WORKING_DIR_PREFIX);

        // merge dynamicParams with events.par then load parametersFile in a runtime tmp directory
        InputStream eventsIs = getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, EVENTS_PAR).toString());
        byte[] eventsParams = eventsIs.readAllBytes();
        if (dynamicParams.length != 0 && eventsParams.length != 0) { // suppose two models .par are valid => do merge
            mergeParams(dynamicParams, eventsParams, workingDir, MODELS_PAR);
        } else { // create without events par
            Files.copy(new ByteArrayInputStream(dynamicParams), workingDir.resolve(MODELS_PAR));
        }

        // load two others files
        for (String parFileName : List.of(NETWORK_PAR, SOLVERS_PAR)) {
            Files.copy(getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, parFileName).toString()), workingDir.resolve(parFileName));
        }

        // load parameter file then config paths
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(getClass().getResourceAsStream(Paths.get(PARAMETERS_DIR, PARAMETERS_JSON).toString()));
        DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);
        dynaWaltzParameters.setParametersFile(workingDir.resolve(MODELS_PAR).toString());
        dynaWaltzParameters.getNetwork().setParametersFile(workingDir.resolve(NETWORK_PAR).toString());
        dynaWaltzParameters.getSolver().setParametersFile(workingDir.resolve(SOLVERS_PAR).toString());
        return parameters;
    }

    private void mergeParams(byte[] dynamicParams, byte[] eventsParams, Path tmpDir, String modelParFileName) throws IOException {
        InputStream dynamicParamsIs = new ByteArrayInputStream(dynamicParams);
        InputStream eventsParamsIs = new ByteArrayInputStream(eventsParams);
        XmlMerge xmlMerge = new SimpleXmlMerge();
        try {
            Document mergedDoc = xmlMerge.merge(dynamicParamsIs, eventsParamsIs);
            xmlMerge.export(mergedDoc, Files.newOutputStream(tmpDir.resolve(modelParFileName)));
        } catch (ParserConfigurationException | SAXException | TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
