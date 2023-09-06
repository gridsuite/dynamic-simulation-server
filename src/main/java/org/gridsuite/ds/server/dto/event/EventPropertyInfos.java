/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.ds.server.utils.PropertyType;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventPropertyInfos {

    private String name;

    private String value;

    private PropertyType type;

}
