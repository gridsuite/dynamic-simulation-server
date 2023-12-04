package org.gridsuite.ds.server.utils;

public enum EquipmentType {
    GENERATOR,
    LOAD,
    BUS,
    STATIC_VAR_COMPENSATOR;

    public static boolean isStaticType(EquipmentType equipmentType) {
        return equipmentType == BUS;
    }
}
