/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.dto.dynamicmapping.automata;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public record Automaton(
    @Schema(description = "Mapped Model Instance ID")
    String model,
    @Schema(description = "Mapped Parameters Set Group ID")
    String setGroup,
    @Schema(description = "Properties of automaton model")
    List<BasicProperty> properties
) {

}
