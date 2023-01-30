/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.utils.xml;

import org.w3c.dom.Document;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO: to remove when when multi input-files implementation in dynawo finished
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface XmlMerge {
    Document merge(InputStream targetIs, InputStream... sourceIsList);

    void export(Document document, OutputStream os);
}
