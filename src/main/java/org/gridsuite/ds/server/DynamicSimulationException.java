/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import lombok.Getter;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
public class DynamicSimulationException extends RuntimeException {

    public enum Type {
        URI_SYNTAX,
        PROVIDER_NOT_FOUND,
        MAPPING_NOT_PROVIDED,
        RESULT_UUID_NOT_FOUND,
        DYNAMIC_MAPPING_NOT_FOUND,
        EXPORT_PARAMETERS_ERROR,
        GET_DYNAMIC_MAPPING_ERROR,
        CREATE_TIME_SERIES_ERROR,
        DELETE_TIME_SERIES_ERROR,
        MAPPING_NOT_LAST_RULE_WITH_EMPTY_FILTER_ERROR,
        DUMP_FILE_ERROR,
        DYNAMIC_SIMULATION_PARAMETERS_ERROR,
        DYNAMIC_MODEL_ERROR,
    }

    private final Type type;

    public DynamicSimulationException(Type type, String message) {
        super(message);
        this.type = type;
    }
}
