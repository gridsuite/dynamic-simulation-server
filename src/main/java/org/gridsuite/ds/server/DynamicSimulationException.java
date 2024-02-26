/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import java.util.Objects;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class DynamicSimulationException extends RuntimeException {

    public enum Type {
        URI_SYNTAX,
        RESULT_UUID_NOT_FOUND,
        DYNAMIC_MAPPING_NOT_FOUND,
        CREATE_MAPPING_SCRIPT_ERROR,
        CREATE_TIME_SERIES_ERROR,
        DELETE_TIME_SERIES_ERROR
    }

    private final Type type;

    public DynamicSimulationException(Type type) {
        super(Objects.requireNonNull(type.name()));
        this.type = type;
    }

    public DynamicSimulationException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
