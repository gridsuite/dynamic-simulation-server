/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.parameters;

import org.gridsuite.ds.server.dto.event.EventInfos;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface EventGroovyGeneratorService {
    String RESOURCE_PATH_DELIMETER = "/";
    String EVENTS_TEMPLATE_DIR = "templates/event";

    /**
     * Generate a script groovy which contains events
     * @param events given list of events
     * @return a script groovy which contains events
     */
    String generate(List<EventInfos> events);
}
