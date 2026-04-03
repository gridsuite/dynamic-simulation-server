/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.curve;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.gridsuite.ds.server.utils.EquipmentType;

import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurveInfos {
    private UUID id;
    private EquipmentType equipmentType;
    private String equipmentId;
    private String variableId;
}
