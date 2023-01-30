/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.utils.xml;

import com.powsybl.commons.exceptions.UncheckedSaxException;
import org.gridsuite.ds.server.utils.xml.implementation.SimpleXmlMerge;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static com.powsybl.commons.ComparisonUtils.compareXml;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class XmlMergeTest {

    public static final String RESOURCE_PATH_DELIMETER = "/";
    public static final String DATA_XML = RESOURCE_PATH_DELIMETER + "data" + RESOURCE_PATH_DELIMETER + "xml";
    public static final String PAR_SCHEMA = "parameters.xsd";
    public static final String INPUT = "input";
    public static final String EVENT_PAR = "events.par";
    public static final String MODEL_PAR = "models.par";

    public static final String OUTPUT = "output";
    public static final String EXPECTED_RESULT = "expected_models.par";
    public static final String EXPORTED_RESULT = "exported_models.par";

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
    public void mergePar() {
        // get source file .par
        InputStream sourceIs = getClass().getResourceAsStream(DATA_XML + RESOURCE_PATH_DELIMETER + INPUT + RESOURCE_PATH_DELIMETER + EVENT_PAR);

        // get target file .par
        InputStream targetIs = getClass().getResourceAsStream(DATA_XML + RESOURCE_PATH_DELIMETER + INPUT + RESOURCE_PATH_DELIMETER + MODEL_PAR);

        // merge two files
        XmlMerge xmlMerge = new SimpleXmlMerge();
        Document mergedDocument = xmlMerge.merge(targetIs, sourceIs);

        // export result
        try {
            String resultDir = getClass().getResource(DATA_XML + RESOURCE_PATH_DELIMETER + OUTPUT).getPath();
            Path resultParFile = Paths.get(resultDir).resolve(EXPORTED_RESULT);
            Files.deleteIfExists(resultParFile);
            Files.createFile(resultParFile);
            xmlMerge.export(mergedDocument, Files.newOutputStream(resultParFile));

            // compare two file
            validate(DATA_XML + RESOURCE_PATH_DELIMETER + PAR_SCHEMA, DATA_XML + RESOURCE_PATH_DELIMETER + OUTPUT + RESOURCE_PATH_DELIMETER + EXPECTED_RESULT, resultParFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }
}
