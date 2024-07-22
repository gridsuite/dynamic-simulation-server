/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.parameters.impl;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.io.IOUtils;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.service.parameters.CurveGroovyGeneratorService;
import org.gridsuite.ds.server.utils.EquipmentType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.gridsuite.ds.server.utils.EquipmentType.isStaticType;
import static org.gridsuite.ds.server.utils.Utils.RESOURCE_PATH_DELIMITER;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class CurveGroovyGeneratorServiceImpl implements CurveGroovyGeneratorService {
    public String generate(List<CurveInfos> curveInfosList) {
        Objects.requireNonNull(curveInfosList);

        String curvesTemplate;
        String curveTemplate;
        try {
            curvesTemplate = IOUtils.toString(new ClassPathResource(CURVES_TEMPLATE_DIR + RESOURCE_PATH_DELIMITER + "curves.st").getInputStream(), Charset.defaultCharset());
            curveTemplate = IOUtils.toString(new ClassPathResource(CURVES_TEMPLATE_DIR + RESOURCE_PATH_DELIMITER + "curve.st").getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new PowsyblException("Unable to load templates for groovy script generation : " + e.getMessage());
        }
        // config root template
        ST curvesST = new ST(curvesTemplate);

        // indexing equipment type by equipmentId
        Map<String, EquipmentType> equipmentTypeByEquipmentIdMap = curveInfosList.stream()
                .collect(Collectors.groupingBy(CurveInfos::getEquipmentId,
                        Collectors.collectingAndThen(Collectors.toSet(), set -> set.stream().findFirst().get().getEquipmentType())));

        // group variables by equipmentId
        Map<String, String> variablesByEquipmentIdMap = curveInfosList.stream()
                .collect(Collectors.groupingBy(CurveInfos::getEquipmentId, LinkedHashMap::new,
                        Collectors.mapping(curveInfo -> "\"" + curveInfo.getVariableId() + "\"", Collectors.joining(", "))));

        String[] curveStringList = variablesByEquipmentIdMap.entrySet().stream().map(variablesByEquipmentId -> {
            ST curveST = new ST(curveTemplate);
            curveST.add("idKey", isStaticType(equipmentTypeByEquipmentIdMap.get(variablesByEquipmentId.getKey())) ? "staticId" : "dynamicModelId");
            curveST.add("idValue", "\"" + variablesByEquipmentId.getKey() + "\"");
            curveST.add("variables", variablesByEquipmentId.getValue());
            return curveST.render();
        }).toArray(String[]::new);
        curvesST.add("curves", curveStringList);

        return curvesST.render();
    }
}
