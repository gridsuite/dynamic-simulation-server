package org.gridsuite.ds.server.utils;

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
