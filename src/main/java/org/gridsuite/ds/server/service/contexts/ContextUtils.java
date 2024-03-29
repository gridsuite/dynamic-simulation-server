/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.contexts;

import com.powsybl.commons.PowsyblException;
import org.springframework.messaging.MessageHeaders;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ContextUtils {

    private ContextUtils() {

    }

    static String getNonNullHeader(MessageHeaders headers, String name) {
        String header = (String) headers.get(name);
        if (header == null) {
            throw new PowsyblException("Header '" + name + "' not found");
        }
        return header;
    }
}
