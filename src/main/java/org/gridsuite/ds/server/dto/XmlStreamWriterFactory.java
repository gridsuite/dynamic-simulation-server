package org.gridsuite.ds.server.dto;

import com.powsybl.dynawaltz.xml.DynaWaltzXmlConstants;
import javanet.staxutils.IndentingXMLStreamWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.io.Writer;

/**
 * TODO update next version powsybl dynawo 1.15.0
 * com.powsybl.dynawaltz.xml.XmlStreamWriterFactory;
 */
public final class XmlStreamWriterFactory {

    private XmlStreamWriterFactory() {
    }

    public static XMLStreamWriter newInstance(Writer writer) throws XMLStreamException {
        return newInstance(writer, true);
    }

    public static XMLStreamWriter newInstance(Writer writer, boolean indent) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(writer);
        return withIndent(xmlStreamWriter, indent);
    }

    public static XMLStreamWriter newInstance(OutputStream os) throws XMLStreamException {
        return newInstance(os, true);
    }

    public static XMLStreamWriter newInstance(OutputStream os, boolean indent) throws XMLStreamException {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        XMLStreamWriter xmlStreamWriter = factory.createXMLStreamWriter(os);
        return withIndent(xmlStreamWriter, indent);
    }

    private static XMLStreamWriter withIndent(XMLStreamWriter xmlStreamWriter, boolean indent) {
        if (indent) {
            IndentingXMLStreamWriter indentingWriter = new IndentingXMLStreamWriter(xmlStreamWriter);
            indentingWriter.setIndent(DynaWaltzXmlConstants.INDENT);
            return indentingWriter;
        }
        return xmlStreamWriter;
    }
}
