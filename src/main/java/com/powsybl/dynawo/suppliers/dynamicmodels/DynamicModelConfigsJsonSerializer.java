/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.dynawo.suppliers.dynamicmodels;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.dynawo.suppliers.Property;
import com.powsybl.dynawo.suppliers.PropertyType;
import com.powsybl.iidm.network.TwoSides;

import java.io.IOException;
import java.util.List;

/**
 * Serialize a List<DynamicModelConfig> with "models" as root:
 *
 * {
 *   "models": [
 *     {
 *       "model": "...",
 *       "group": "...",
 *       "groupType": "FIXED",
 *       "properties": [ ... ]
 *     }
 *   ]
 * }
 *
 * This matches {@link DynamicModelConfigsJsonDeserializer}.
 *
 * TODO : to remove when available at powsybl-dynawo
 *
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicModelConfigsJsonSerializer extends StdSerializer<List<DynamicModelConfig>> {

    public DynamicModelConfigsJsonSerializer() {
        super((Class<List<DynamicModelConfig>>) (Class<?>) List.class);
    }

    @Override
    public void serialize(List<DynamicModelConfig> configs,
                          JsonGenerator gen,
                          SerializerProvider provider) throws IOException {

        gen.writeStartObject();

        gen.writeFieldName("models");
        writeDynamicModelConfigs(configs, gen);

        gen.writeEndObject();
    }

    private static void writeDynamicModelConfigs(List<DynamicModelConfig> configs, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        if (configs != null) {
            for (DynamicModelConfig cfg : configs) {
                writeDynamicModelConfig(cfg, gen);
            }
        }
        gen.writeEndArray();
    }

    private static void writeDynamicModelConfig(DynamicModelConfig cfg,
                                                JsonGenerator gen) throws IOException {

        gen.writeStartObject();

        gen.writeStringField("model", cfg.model());
        gen.writeStringField("group", cfg.group());

        if (cfg.groupType() != null) {
            gen.writeStringField("groupType", cfg.groupType().name());
        }

        gen.writeFieldName("properties");
        writeProperties(cfg.properties(), gen);

        gen.writeEndObject();
    }

    private static void writeProperties(List<Property> properties, JsonGenerator gen) throws IOException {
        gen.writeStartArray();
        if (properties != null) {
            for (Property p : properties) {
                writeProperty(p, gen);
            }
        }
        gen.writeEndArray();
    }

    /**
     * See {@code PropertyParserUtils.parseProperty}:
     * - always writes "name"
     * - writes exactly one of: "value" (string value), "values" (string array), "arrays" (array of string arrays)
     * - writes "type" when it can be inferred from propertyClass
     */
    private static void writeProperty(Property property, JsonGenerator gen) throws IOException {
        gen.writeStartObject();

        gen.writeStringField("name", property.name());

        Object value = property.value();
        if (value instanceof List<?> list) {
            if (!list.isEmpty()) {
                // lists: List<String|int|double|boolean|TwoSides>
                gen.writeFieldName("values");
                gen.writeStartArray();
                for (Object v : list) {
                    gen.writeString(String.valueOf(v));
                }
                gen.writeEndArray();
                writeOptionalType(gen, property);
            }
        } else if (value != null && value.getClass().isArray()) {
            // arrays: List<List<String|integer|double|boolean|TwoSides>>
            gen.writeFieldName("arrays");
            gen.writeStartArray();
            for (List<?> row : (List<?>[]) value) {
                gen.writeStartArray();
                for (Object v : row) {
                    gen.writeString(String.valueOf(v));
                }
                gen.writeEndArray();
            }
            gen.writeEndArray();
            writeOptionalType(gen, property);
        } else {
            if (value instanceof TwoSides ts) {
                gen.writeStringField("value", ts.name());
                writeOptionalType(gen, property);
            } else if (value != null) {
                gen.writeStringField("value", String.valueOf(value));
                writeOptionalType(gen, property);
            }
        }

        gen.writeEndObject();
    }

    /**
     * Writes the "type" field if we can (or if Property already carries an explicit type via propertyClass()).
     *
     * This keeps compatibility with PropertyParserUtils:
     *   case "type" -> builder.type(PropertyType.valueOf(parser.nextTextValue()));
     */
    private static void writeOptionalType(JsonGenerator gen, Property property) throws IOException {
        PropertyType inferredType = PropertyType.STRING;

        Class<?> propertyClass = property.propertyClass();
        if (propertyClass != null) {
            if (propertyClass == double.class) {
                inferredType = PropertyType.DOUBLE;
            } else if (propertyClass == int.class) {
                inferredType = PropertyType.INTEGER;
            } else if (propertyClass == boolean.class) {
                inferredType = PropertyType.BOOLEAN;
            } else if (propertyClass == TwoSides.class) {
                inferredType = PropertyType.TWO_SIDES;
            }
        }

        gen.writeStringField("type", inferredType.name());
    }
}
