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
class DynamicSimulationException  extends RuntimeException {

    public enum Type {
        DATABASE_DRIVER_NOT_FOUND,
    }

    private final Type type;

    DynamicSimulationException(Type type) {
        super(Objects.requireNonNull(type.name()));
        this.type = type;
    }

    Type getType() {
        return type;
    }
}
