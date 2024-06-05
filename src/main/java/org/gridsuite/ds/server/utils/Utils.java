/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.utils;

import com.powsybl.dynawaltz.suppliers.Property;
import com.powsybl.dynawaltz.suppliers.PropertyBuilder;
import com.powsybl.dynawaltz.suppliers.PropertyType;
import com.powsybl.iidm.network.TwoSides;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.ds.server.dto.dynamicmapping.automata.BasicProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class Utils {
    private Utils() {
        // never called
    }

    public static List<String> convertStringToList(String stringArray) {
        List<String> converted = new ArrayList<>();
        converted.addAll(Arrays.asList(stringArray.split(",")));
        return converted;
    }

    public static Property convertProperty(BasicProperty property) {
        String value = property.value();
        PropertyBuilder propertyBuilder = new PropertyBuilder()
                .name(property.name());

        if (property.type() == org.gridsuite.ds.server.utils.PropertyType.ENUM) {
            // using value to infer the enum
            if (StringUtils.isEmpty(value) || value.split(".").length != 2) {
                return null;
            }

            String[] splitValue = value.split(".");
            String enumType = splitValue[0];
            String enumValue = splitValue[1];

            // at moment process only TwoSides
            if (TwoSides.class.getName().equals(enumType)) {
                propertyBuilder.value(enumValue)
                    .type(PropertyType.TWO_SIDES);
            } else {
                return null;
            }
        } else if (property.type() == org.gridsuite.ds.server.utils.PropertyType.STRING) {
            List<String> values = convertStringToList(value);
            // check whether having multiple values
            if (values.size() > 1) {
                propertyBuilder.values(values)
                    .type(PropertyType.STRINGS);
            } else {
                propertyBuilder.value(value)
                    .type(PropertyType.valueOf(property.type().name()));
            }
        } else {
            propertyBuilder.value(value)
                .type(PropertyType.valueOf(property.type().name()));
        }

        return propertyBuilder.build();
    }
}
