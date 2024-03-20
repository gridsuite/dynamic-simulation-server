/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.iidm.network.Generator
import com.powsybl.iidm.network.StaticVarCompensator
import com.powsybl.iidm.network.Load
import com.powsybl.iidm.network.TwoSides

for (Generator equipment : network.generators) {
    if (equipment.terminal.voltageLevel.nominalV == 13.800000) {
        GeneratorSynchronousThreeWindingsProportionalRegulations {
            staticId equipment.id
            parameterSetId  "IEEE14" + equipment.id
        }
    } else      if (equipment.terminal.voltageLevel.nominalV == 69.000000) {
        GeneratorSynchronousFourWindingsProportionalRegulations {
            staticId equipment.id
            parameterSetId  "IEEE14" + equipment.id
        }
    } else      if (true) {
        GeneratorPQ {
            staticId equipment.id
            parameterSetId  "GPQ"
        }
    }
}

for (StaticVarCompensator equipment : network.staticVarCompensators) {
    if (equipment.terminal.voltageLevel.nominalV == 69.000000) {
        StaticVarCompensator {
            staticId equipment.id
            parameterSetId  "SVarCT"
        }
    }
}

for (Load equipment : network.loads) {
    if (true) {
        LoadAlphaBeta {
            staticId equipment.id
            parameterSetId  "LAB"
        }
    }
}

CurrentLimitAutomaton {
    parameterSetId "CLA_2_4"
    dynamicModelId "CLA_1"
    iMeasurement "_BUS____2-BUS____4-1_AC"
    iMeasurementSide TwoSides.TWO
    controlledQuadripole "_BUS____2-BUS____4-1_AC"
}

CurrentLimitAutomaton {
    parameterSetId "CLA_2_5"
    dynamicModelId "CLA_2"
    iMeasurement "_BUS____2-BUS____5-1_AC"
    iMeasurementSide TwoSides.TWO
    controlledQuadripole "_BUS____2-BUS____5-1_AC"
}