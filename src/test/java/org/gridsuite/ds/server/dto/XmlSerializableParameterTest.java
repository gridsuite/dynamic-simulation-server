/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto;

import com.powsybl.commons.exceptions.UncheckedSaxException;
import org.gridsuite.ds.server.DynamicSimulationApplication;
import org.gridsuite.ds.server.dto.solver.IdaSolverInfos;
import org.gridsuite.ds.server.dto.solver.SimSolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;
import org.gridsuite.ds.server.dto.solver.SolverTypeInfos;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringRunner;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.powsybl.commons.test.ComparisonUtils.compareXml;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextHierarchy({@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class})})
public class XmlSerializableParameterTest {

    public static final String RESOURCE_PATH_DELIMETER = "/";
    public static final String DATA_XML = RESOURCE_PATH_DELIMETER + "data" + RESOURCE_PATH_DELIMETER + "xml";
    public static final String PAR_SCHEMA = "parameters.xsd";

    public static final String OUTPUT = "output";
    public static final String EXPECTED_SOLVERS = "expected_solvers.par";
    public static final String EXPORTED_SOLVERS = "exported_solvers.par";

    public static final String EXPECTED_NETWORK = "expected_network.par";
    public static final String EXPORTED_NETWORK = "exported_network.par";

    private void validate(String schemaFile, String expectedXmlFile, Path actualXmlFile) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Source xml = new StreamSource(Files.newInputStream(actualXmlFile));
            Source xsd = new StreamSource(getClass().getResourceAsStream(schemaFile));
            Schema schema = factory.newSchema(xsd);
            Validator validator = schema.newValidator();
            validator.validate(xml);
            compareXml(getClass().getResourceAsStream(expectedXmlFile), Files.newInputStream(actualXmlFile));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SAXException e) {
            throw new UncheckedSaxException(e);
        }
    }

    @Test
    public void testWriteParameterGivenSolvers() throws IOException, XMLStreamException {

        IdaSolverInfos idaSolver = new IdaSolverInfos();
        idaSolver.setId("2");
        idaSolver.setType(SolverTypeInfos.IDA);
        idaSolver.setOrder(2);
        idaSolver.setInitStep(0.000001);
        idaSolver.setMinStep(0.000001);
        idaSolver.setMaxStep(10);
        idaSolver.setAbsAccuracy(0.0001);
        idaSolver.setRelAccuracy(0.0001);

        SimSolverInfos simSolver = new SimSolverInfos();
        simSolver.setId("3");
        simSolver.setType(SolverTypeInfos.SIM);
        simSolver.setHMin(0.000001);
        simSolver.setHMax(1);
        simSolver.setKReduceStep(0.5);
        simSolver.setNEff(10);
        simSolver.setNDeadband(2);
        simSolver.setMaxRootRestart(3);
        simSolver.setMaxNewtonTry(10);
        simSolver.setLinearSolverName("KLU");
        simSolver.setRecalculateStep(false);

        SolverInfos[] solvers = {idaSolver, simSolver};

        // export solvers to par file
        String resultDir = getClass().getResource(DATA_XML + RESOURCE_PATH_DELIMETER + OUTPUT).getPath();
        Path exportedSolversFile = Paths.get(resultDir).resolve(EXPORTED_SOLVERS);
        Files.deleteIfExists(exportedSolversFile);
        XmlSerializableParameter.writeParameter(exportedSolversFile, XmlSerializableParameter.PARAMETER_SET, solvers);

        // compare two file
        validate(DATA_XML + RESOURCE_PATH_DELIMETER + PAR_SCHEMA, DATA_XML + RESOURCE_PATH_DELIMETER + OUTPUT + RESOURCE_PATH_DELIMETER + EXPECTED_SOLVERS, exportedSolversFile);
    }

    @Test
    public void testWriteParameterGivenNetwork() throws IOException, XMLStreamException {
        NetworkInfos network = new NetworkInfos();
        network.setCapacitorNoReclosingDelay(300);
        network.setDanglingLineCurrentLimitMaxTimeOperation(90);
        network.setLineCurrentLimitMaxTimeOperation(90);
        network.setLoadTp(90);
        network.setLoadTq(90);
        network.setLoadAlpha(1);
        network.setLoadAlphaLong(0);
        network.setLoadBeta(2);
        network.setLoadBetaLong(0);
        network.setLoadIsControllable(false);
        network.setLoadIsRestorative(false);
        network.setLoadZPMax(100);
        network.setLoadZQMax(100);
        network.setReactanceNoReclosingDelay(0);
        network.setTransformerCurrentLimitMaxTimeOperation(90);
        network.setTransformerT1StHT(60);
        network.setTransformerT1StTHT(30);
        network.setTransformerTNextHT(10);
        network.setTransformerTNextTHT(10);
        network.setTransformerTolV(0.015);

        // export network to par file
        String resultDir = getClass().getResource(DATA_XML + RESOURCE_PATH_DELIMETER + OUTPUT).getPath();
        Path exportedNetworkFile = Paths.get(resultDir).resolve(EXPORTED_NETWORK);
        Files.deleteIfExists(exportedNetworkFile);
        XmlSerializableParameter.writeParameter(exportedNetworkFile, XmlSerializableParameter.PARAMETER_SET, network);

        // compare two file
        validate(DATA_XML + RESOURCE_PATH_DELIMETER + PAR_SCHEMA, DATA_XML + RESOURCE_PATH_DELIMETER + OUTPUT + RESOURCE_PATH_DELIMETER + EXPECTED_NETWORK, exportedNetworkFile);
    }
}
