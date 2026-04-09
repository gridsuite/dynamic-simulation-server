/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller.utils;

import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.event.EventPropertyInfos;
import org.gridsuite.ds.server.utils.EquipmentType;
import org.gridsuite.ds.server.utils.PropertyType;

import java.util.List;

import static org.gridsuite.ds.server.service.parameters.impl.ParametersServiceImpl.FIELD_STATIC_ID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class ParameterTestUtils {
    private ParameterTestUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static List<CurveInfos> getCurveInfosList() {
        return List.of(
                new CurveInfos(null, EquipmentType.LOAD, "_LOAD___2_EC", "load_PPu"),
                new CurveInfos(null, EquipmentType.LOAD, "_LOAD___2_EC", "load_QPu"),
                new CurveInfos(null, EquipmentType.GENERATOR, "_GEN____3_SM", "generator_omegaPu"),
                new CurveInfos(null, EquipmentType.GENERATOR, "_GEN____3_SM", "generator_PGen"),
                new CurveInfos(null, EquipmentType.GENERATOR, "_GEN____3_SM", "generator_QGen"),
                new CurveInfos(null, EquipmentType.GENERATOR, "_GEN____3_SM", "generator_UStatorPu"),
                new CurveInfos(null, EquipmentType.GENERATOR, "_GEN____3_SM", "voltageRegulator_EfdPu"),
                new CurveInfos(null, EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_UPu"),
                new CurveInfos(null, EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_PInjPu"),
                new CurveInfos(null, EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_QInjPu"),
                new CurveInfos(null, EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_injector_BPu"),
                new CurveInfos(null, EquipmentType.STATIC_VAR_COMPENSATOR, "SVC2", "SVarC_modeHandling_mode_value")
        );
    }

    public static List<EventInfos> getEventInfosList() {
        return List.of(
                new EventInfos(null, null, "_BUS____1-BUS____5-1_AC", null, "Disconnect", List.of(
                        new EventPropertyInfos(null, FIELD_STATIC_ID, "_BUS____1-BUS____5-1_AC", PropertyType.STRING),
                        new EventPropertyInfos(null, "startTime", "10", PropertyType.FLOAT),
                        new EventPropertyInfos(null, "disconnectOnly", "TwoSides.TWO", PropertyType.ENUM)
                )),
                new EventInfos(null, null, "_BUS____1_TN", null, "FaultNode", List.of(
                        new EventPropertyInfos(null, FIELD_STATIC_ID, "_BUS____1_TN", PropertyType.STRING),
                        new EventPropertyInfos(null, "startTime", "20", PropertyType.FLOAT),
                        new EventPropertyInfos(null, "faultTime", "2", PropertyType.FLOAT),
                        new EventPropertyInfos(null, "rPu", "23", PropertyType.FLOAT),
                        new EventPropertyInfos(null, "xPu", "32", PropertyType.FLOAT)
                ))
        );
    }
}
