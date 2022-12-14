/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.utils.xml.implementation;

import org.gridsuite.ds.server.utils.xml.XmlMerge;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class SimpleXmlMerge implements XmlMerge {
    @Override
    public Document merge(InputStream targetIs, InputStream... sourceIsList) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        // to be compliant, completely disable DOCTYPE declaration:
        documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        // get all root elements of source input
        List<Element> sourceRootList = new ArrayList<>();
        for (InputStream sourceIs : sourceIsList) {
            Document source = documentBuilder.parse(sourceIs);
            sourceRootList.add(source.getDocumentElement());
        }

        // get the root element of target input
        Document target = documentBuilder.parse(targetIs);
        Element targetRoot = target.getDocumentElement();

        // merge attributes
        // do nothing at the moment, take the same as target

        // merge child nodes
        for (Element sourceRoot : sourceRootList) {
            NodeList sourceChildNodes = sourceRoot.getChildNodes();
            for (int i = 0; i < sourceChildNodes.getLength(); i++) {
                Node importedNode = target.importNode(sourceChildNodes.item(i), true);
                targetRoot.appendChild(importedNode);
            }
        }

        return target;
    }

    @Override
    public void export(Document document, OutputStream os) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        // to be compliant, prohibit the use of all protocols by external entities:
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(os);
        transformer.transform(source, result);
    }
}
