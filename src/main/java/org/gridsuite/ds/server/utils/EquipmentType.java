/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.utils;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public enum EquipmentType {
    GENERATOR,
    LOAD,
    BUS,
    BUSBAR_SECTION,
    STATIC_VAR_COMPENSATOR;

    public static boolean isStaticType(EquipmentType equipmentType) {
        return equipmentType == BUS || equipmentType == BUSBAR_SECTION;
    }
}
