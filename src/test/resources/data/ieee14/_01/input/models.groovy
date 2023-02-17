/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.iidm.network.Load
import com.powsybl.iidm.network.Generator
import com.powsybl.dynawaltz.models.automatons.CurrentLimitAutomaton
import com.powsybl.iidm.network.Branch
import com.powsybl.dynawaltz.models.automatons.CurrentLimitAutomaton
import com.powsybl.iidm.network.Branch

for (Load equipment : network.loads) {
          if (true) {
                 LoadAlphaBeta {
                     staticId equipment.id
                     parameterSetId  "LAB"
                 }
    }

}

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

CurrentLimitAutomaton {
     staticId "_BUS____2-BUS____4-1_AC"
     dynamicModelId "CurrentLimitAutomaton24"
     parameterSetId "CLA_2_4"
     side Branch.Side.TWO
}

CurrentLimitAutomaton {
     staticId "_BUS____2-BUS____5-1_AC"
     dynamicModelId "CurrentLimitAutomaton25"
     parameterSetId "CLA_2_5"
     side Branch.Side.TWO
}
