/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.dynamicmapping.automata;

import com.powsybl.dynawaltz.suppliers.PropertyType;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public record BasicProperty(String name, String value, PropertyType type) {

}
