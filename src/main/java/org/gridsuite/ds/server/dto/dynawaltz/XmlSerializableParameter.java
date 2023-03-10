/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.dynawaltz;

import com.powsybl.dynawaltz.xml.XmlStreamWriterFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public interface XmlSerializableParameter {
    String DYN_BASE_URI = "http://www.rte-france.com/dynawo";
    String PARAMETER_SET = "parametersSet";

    enum ParameterType {
        DOUBLE,
        INT,
        BOOL,
        STRING;
    }

    void writeParameter(XMLStreamWriter writer) throws XMLStreamException;

    static void writeParameter(XMLStreamWriter writer, ParameterType type, String name, String value) throws XMLStreamException {
        writer.writeEmptyElement("par");
        writer.writeAttribute("type", type.toString());
        writer.writeAttribute("name", name);
        writer.writeAttribute("value", value);
    }

    static void writeParameter(Path file, String rootElementName, XmlSerializableParameter... objects) throws IOException, XMLStreamException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(rootElementName);
        Objects.requireNonNull(objects);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
            XMLStreamWriter xmlWriter = XmlStreamWriterFactory.newInstance(writer);
            try {
                xmlWriter.writeStartDocument(StandardCharsets.UTF_8.toString(), "1.0");
                xmlWriter.setPrefix("", DYN_BASE_URI);
                xmlWriter.writeStartElement(DYN_BASE_URI, rootElementName);
                xmlWriter.writeNamespace("", DYN_BASE_URI);

                for (XmlSerializableParameter object : objects) {
                    object.writeParameter(xmlWriter);
                }

                xmlWriter.writeEndElement();
                xmlWriter.writeEndDocument();
            } finally {
                xmlWriter.close();
            }
        }
    }
}
