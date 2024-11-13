/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.utils;

import com.powsybl.dynawo.suppliers.Property;
import com.powsybl.dynawo.suppliers.PropertyBuilder;
import com.powsybl.dynawo.suppliers.PropertyType;
import com.powsybl.iidm.network.TwoSides;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.ds.server.dto.dynamicmapping.automata.BasicProperty;
import org.gridsuite.ds.server.dto.event.EventPropertyInfos;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public final class Utils {

    public static final String RESOURCE_PATH_DELIMITER = "/";

    private Utils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static List<String> convertStringToList(String stringArray) {
        List<String> converted = new ArrayList<>();
        converted.addAll(Arrays.asList(stringArray.split(",")));
        return converted;
    }

    public static Property convertProperty(EventPropertyInfos property) {
        return convertProperty(new BasicProperty(
            property.getName(),
            property.getValue(),
            property.getType()));
    }

    public static Property convertProperty(BasicProperty property) {
        String value = property.value();
        PropertyBuilder propertyBuilder = new PropertyBuilder()
                .name(property.name());
        if (property.type() == org.gridsuite.ds.server.utils.PropertyType.FLOAT) {
            // powsybl-dynawo does not support FLOAT => use DOUBLE
            propertyBuilder.value(value)
                .type(PropertyType.DOUBLE);
        } else if (property.type() == org.gridsuite.ds.server.utils.PropertyType.ENUM) {
            // using value to infer enum type
            if (StringUtils.isEmpty(value) || value.split("\\.").length != 2) {
                return null;
            }

            String[] splitValue = value.split("\\.");
            String enumType = splitValue[0];
            String enumValue = splitValue[1];

            // at moment process only TwoSides, e.g. TwoSides.TWO or TwoSides.ONE
            if (TwoSides.class.getSimpleName().equals(enumType)) {
                propertyBuilder.value(enumValue)
                    .type(PropertyType.TWO_SIDES);
            } else {
                return null;
            }
        } else if (property.type() == org.gridsuite.ds.server.utils.PropertyType.STRING) {
            propertyBuilder.value(value)
                .type(PropertyType.valueOf(property.type().name()));
            List<String> values = convertStringToList(value).stream().map(StringUtils::trim).toList();
            // check whether having multiple values
            if (values.size() > 1) {
                // override by set multiple values
                propertyBuilder.values(values);
            }
        } else {
            propertyBuilder.value(value)
                .type(PropertyType.valueOf(property.type().name()));
        }

        return propertyBuilder.build();
    }

    public static byte[] zip(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath);
             ByteArrayOutputStream os = new ByteArrayOutputStream();
             ZipOutputStream zipOs = new ZipOutputStream(os)) {
            zipOs.putNextEntry(new ZipEntry(filePath.getFileName().toString()));
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                zipOs.write(buffer, 0, length);
            }
            zipOs.closeEntry();
            return os.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while zipping the file " + filePath.toAbsolutePath(), e);
        }
    }

    public static void unzip(byte[] zippedBytes, Path filePath) {
        try (ByteArrayInputStream is = new ByteArrayInputStream(zippedBytes);
             FileOutputStream fos = new FileOutputStream(new File(filePath.toUri()));
             ZipInputStream zipIs = new ZipInputStream(is)) {
            if (zipIs.getNextEntry() != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zipIs.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }
                zipIs.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error occurred while unzipping a zipped content to the file " + filePath.toAbsolutePath(), e);
        }
    }
}
