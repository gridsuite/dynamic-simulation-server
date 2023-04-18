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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

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
            curvesTemplate = IOUtils.toString(new ClassPathResource(CURVES_TEMPLATE_DIR + RESOURCE_PATH_DELIMETER + "curves.st").getInputStream(), Charset.defaultCharset());
            curveTemplate = IOUtils.toString(new ClassPathResource(CURVES_TEMPLATE_DIR + RESOURCE_PATH_DELIMETER + "curve.st").getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new PowsyblException("Unable to load templates for groovy script generation : " + e.getMessage());
        }
        // config root template
        ST curvesST = new ST(curvesTemplate);

        // group curves info by equipmentId
        Map<String, String> variablesByEquipmentIdMap = curveInfosList.stream()
                .collect(Collectors.groupingBy(CurveInfos::getEquipmentId, Collectors.mapping(curveInfo -> {
                    // format of complete variable id : modelName/<variableSetGroup>/<variableSet>/variableId
                    String[] variablePaths = curveInfo.getVariableId().split(VARIABLE_PATH_DELIMETER);
                    // get only the last, i.e. variable id
                    return "\""  + variablePaths[variablePaths.length - 1] + "\"";
                }, Collectors.joining(", "))));

        String [] curveStringList = variablesByEquipmentIdMap.entrySet().stream().map(variablesByEquipmentId -> {
            ST curveST = new ST(curveTemplate);
            curveST.add("dynamicModelId", "\"" + variablesByEquipmentId.getKey() + "\"");
            curveST.add("variables", variablesByEquipmentId.getValue());
            return curveST.render();
        }).toArray(String[]::new);
        curvesST.add("curves", curveStringList);

        return curvesST.render();
    }
}
