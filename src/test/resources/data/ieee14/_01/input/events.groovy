package data.ieee14._01.input
/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.iidm.network.Line


for (Line line : network.lines) {
    if (line.id == "_BUS____1-BUS____5-1_AC") {
        EventQuadripoleDisconnection {
            staticId line.id
            eventModelId "DISCONNECT_LINE"
            parameterSetId "EQD"
        }
    }
}
