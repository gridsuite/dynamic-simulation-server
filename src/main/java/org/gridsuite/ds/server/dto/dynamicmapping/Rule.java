/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.dto.dynamicmapping;

import com.powsybl.dynawaltz.rte.mapping.dynamicmodels.SetGroupType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.gridsuite.filter.expertfilter.ExpertFilter;
import org.gridsuite.filter.utils.EquipmentType;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Schema(description = "Rule")
public record Rule(
    @Schema(description = "Equipment type")
    EquipmentType equipmentType,

    @Schema(description = "Mapped Model Instance ID")
    String mappedModel,

    @Schema(description = "Mapped Parameter Set Group ID")
    String setGroup,

    @Schema(description = "Mapped Parameter Set Group Type")
    SetGroupType groupType,

    @Schema(description = "Filter")
    ExpertFilter filter
) {

}

