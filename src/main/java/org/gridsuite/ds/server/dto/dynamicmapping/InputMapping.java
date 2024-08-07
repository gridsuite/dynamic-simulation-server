/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.dto.dynamicmapping;

import io.swagger.v3.oas.annotations.media.Schema;
import org.gridsuite.ds.server.dto.dynamicmapping.automata.Automaton;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Schema(description = "Mapping")
public record InputMapping(
    @Schema(description = "Name")
    String name,
    @Schema(description = "Rules")
    List<Rule> rules,
    @Schema(description = "Automata")
    List<Automaton> automata
) {

}
