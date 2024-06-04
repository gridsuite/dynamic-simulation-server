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

    public static Property convertToProperty(BasicProperty property) {
        String value = property.value();
        PropertyType type = property.type();
        PropertyBuilder propertyBuilder = new PropertyBuilder()
                .name(property.name())
                .type(type)
                .value(value);
        if (type == PropertyType.STRING) {
            List<String> values = convertStringToList(value);
            // check whether having multiple values
            if (values.size() > 1) {
                // override single string by multi strings
                propertyBuilder.type(PropertyType.STRINGS);
                propertyBuilder.values(values);
            }
        }
        return propertyBuilder.build();
    }
}
