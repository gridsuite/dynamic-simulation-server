/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.utils;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public enum SetGroupType {
    /**
     * group is used as a parameter set id
     */
    FIXED,
    /**
     * the parameter set id comprise the model dynamic id (or the equipment static id if not found) prefixed by group
     */
    PREFIX,
    /**
     * the parameter set id comprise the model dynamic id (or the equipment static id if not found) suffixed by group
     */
    SUFFIX
}
