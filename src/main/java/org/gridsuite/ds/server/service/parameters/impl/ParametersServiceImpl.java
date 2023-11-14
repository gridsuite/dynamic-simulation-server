/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters.impl;

import com.powsybl.commons.exceptions.UncheckedXmlStreamException;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import com.powsybl.dynawaltz.DynaWaltzParameters;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import com.powsybl.dynawaltz.xml.ParametersXml;
import com.powsybl.dynawaltz.parameters.ParametersSet;
import org.apache.commons.lang3.ArrayUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.XmlSerializableParameter;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.network.NetworkInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;
import org.gridsuite.ds.server.service.parameters.CurveGroovyGeneratorService;
import org.gridsuite.ds.server.service.parameters.EventGroovyGeneratorService;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersServiceImpl implements ParametersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersServiceImpl.class);

    private final CurveGroovyGeneratorService curveGroovyGeneratorService;

    private final EventGroovyGeneratorService eventGroovyGeneratorService;

    @Autowired
    public ParametersServiceImpl(CurveGroovyGeneratorService curveGroovyGeneratorService, EventGroovyGeneratorService eventGroovyGeneratorService) {
        this.curveGroovyGeneratorService = curveGroovyGeneratorService;
        this.eventGroovyGeneratorService = eventGroovyGeneratorService;
    }

    @Override
    public byte[] getEventModel(List<EventInfos> events) {
        byte[] eventModel = new byte[0];
        if (events != null) {
            String generatedGroovyEvents = eventGroovyGeneratorService.generate(events);
            LOGGER.info(generatedGroovyEvents);
            eventModel = generatedGroovyEvents.getBytes(StandardCharsets.UTF_8);
        }

        return eventModel;
    }

    @Override
    public byte[] getCurveModel(List<CurveInfos> curves) {
        byte[] curveModel = new byte[0];
        if (curves != null) {
            String generatedGroovyCurves = curveGroovyGeneratorService.generate(curves);
            LOGGER.info(generatedGroovyCurves);
            curveModel = generatedGroovyCurves.getBytes(StandardCharsets.UTF_8);
        }

        return curveModel;
    }

    @Override
    public DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams, String provider, DynamicSimulationParametersInfos inputParameters) {
        try {
            // load parameter file
            DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(getClass().getResourceAsStream(PARAMETERS_DIR + RESOURCE_PATH_DELIMETER + PARAMETERS_JSON));
            // TODO: Powsybl side - create an explicit dependency to DynaWaltz class and keep dynamic simulation abstraction all over this micro service
            if (DynaWaltzProvider.NAME.equals(provider)) {
                // --- MODEL PAR --- //
                List<ParametersSet> modelsParameters = !ArrayUtils.isEmpty(dynamicParams) ? ParametersXml.load(new ByteArrayInputStream(dynamicParams)) : List.of();

                DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);
                dynaWaltzParameters.setModelsParameters(modelsParameters);

                // --- SOLVER PAR --- //
                // solver from input parameter
                SolverInfos inputSolver = inputParameters.getSolvers().stream().filter(elem -> elem.getId().equals(inputParameters.getSolverId())).findFirst().orElse(null);
                if (inputSolver != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, inputSolver);
                    ParametersSet solverParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), inputSolver.getId());
                    dynaWaltzParameters.setSolverType(inputSolver.getType().toSolverType());
                    dynaWaltzParameters.setSolverParameters(solverParameters);
                }


                // --- NETWORK PAR --- //
                // network from input parameters
                NetworkInfos network = inputParameters.getNetwork();
                if (network != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, network);
                    ParametersSet networkParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), network.getId());
                    dynaWaltzParameters.setNetworkParameters(networkParameters);
                }


                // Quick fix to make working in powsybl-dynawo 2.1.0
                // Cannot invoke "com.powsybl.dynawaltz.DumpFileParameters.useDumpFile()"
                // because "dumpFileParameters" is null
                // TODO This param must be configured by default during the creation at the powsybl level
                dynaWaltzParameters.setDefaultDumpFileParameters();
            }

            return parameters;
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
    }
}
