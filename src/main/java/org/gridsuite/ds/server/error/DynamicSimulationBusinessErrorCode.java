/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
public enum DynamicSimulationBusinessErrorCode implements BusinessErrorCode {
    PROVIDER_NOT_FOUND("dynamicSimulation.providerNotFound"),
    MAPPING_NOT_PROVIDED("dynamicSimulation.mappingNotProvided"),
    MAPPING_NOT_LAST_RULE_WITH_EMPTY_FILTER_ERROR("dynamicSimulation.mappingNotLastRuleWithEmptyFilterError");

    private final String code;

    DynamicSimulationBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}
