/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.utils.xml;

import org.gridsuite.ds.server.utils.xml.implementation.SimpleXmlMerge;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static com.powsybl.commons.ComparisonUtils.compareXml;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class XmlMergeTest {

    public static final String DATA_XML = "/data/xml";
    public static final String PAR_SCHEMA = "parameters.xsd";
    public static final String INPUT = "input";
    public static final String EVENT_PAR = "events.par";
    public static final String MODEL_PAR = "models.par";

    public static final String OUTPUT = "output";
    public static final String EXPECTED_RESULT = "expected_models.par";
    public static final String EXPORTED_RESULT = "exported_models.par";

    private void validate(Path schemaFile, Path expectedXmlFile, Path actualXmlFile) throws SAXException, IOException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source xml = new StreamSource(Files.newInputStream(actualXmlFile));
        Source xsd = new StreamSource(getClass().getResourceAsStream(schemaFile.toString()));
        Schema schema = factory.newSchema(xsd);
        Validator validator = schema.newValidator();
        validator.validate(xml);
        compareXml(getClass().getResourceAsStream(expectedXmlFile.toString()), Files.newInputStream(actualXmlFile));
    }

    @Test
    public void mergePar() throws ParserConfigurationException, IOException, SAXException, TransformerException {
        // get source file .par
        InputStream sourceIs = getClass().getResourceAsStream(Paths.get(DATA_XML, INPUT, EVENT_PAR).toString());

        // get target file .par
        InputStream targetIs = getClass().getResourceAsStream(Paths.get(DATA_XML, INPUT, MODEL_PAR).toString());

        // merge two files
        XmlMerge xmlMerge = new SimpleXmlMerge();
        Document mergedDocument = xmlMerge.merge(targetIs, sourceIs);

        // export result
        String resultDir = getClass().getResource(Paths.get(DATA_XML, OUTPUT).toString()).getPath();
        Path resultParFile = Paths.get(resultDir).resolve(EXPORTED_RESULT);
        Files.deleteIfExists(resultParFile);
        Files.createFile(resultParFile);
        xmlMerge.export(mergedDocument, Files.newOutputStream(resultParFile));

        // compare two file
        validate(Paths.get(DATA_XML, PAR_SCHEMA), Paths.get(DATA_XML, OUTPUT, EXPECTED_RESULT), resultParFile);
    }
}
