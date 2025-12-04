/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.error;

import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.gridsuite.ds.server.error.DynamicSimulationBusinessErrorCode.MAPPING_NOT_LAST_RULE_WITH_EMPTY_FILTER_ERROR;
import static org.gridsuite.ds.server.error.DynamicSimulationBusinessErrorCode.MAPPING_NOT_PROVIDED;
import static org.gridsuite.ds.server.error.DynamicSimulationBusinessErrorCode.PROVIDER_NOT_FOUND;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
class DynamicSimulationExceptionHandlerTest {
    private DynamicSimulationExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DynamicSimulationExceptionHandler(() -> "dynamicSimulation");
    }

    @Test
    void mapsMappingNotProvidedErrorBusinessErrorToStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dynamic-simulation/mappings");
        DynamicSimulationException exception = new DynamicSimulationException(MAPPING_NOT_PROVIDED, "Mapping not provided");
        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleDynamicSimulationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        Assertions.assertEquals("dynamicSimulation.mappingNotProvided", response.getBody().getBusinessErrorCode());
    }

    @Test
    void mapsMappingNotLastRuleWithEmptyFilterErrorBusinessErrorToStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dynamic-simulation/mappings");
        DynamicSimulationException exception = new DynamicSimulationException(MAPPING_NOT_LAST_RULE_WITH_EMPTY_FILTER_ERROR, "Only last rule can have empty filter.");
        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleDynamicSimulationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        Assertions.assertEquals("dynamicSimulation.mappingNotLastRuleWithEmptyFilterError", response.getBody().getBusinessErrorCode());
    }

    @Test
    void mapsBadRequestErrorBusinessErrorToStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/dynamic-simulation");
        DynamicSimulationException exception = new DynamicSimulationException(PROVIDER_NOT_FOUND, "Dynamic simulation provider not found");
        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleDynamicSimulationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        Assertions.assertEquals("dynamicSimulation.providerNotFound", response.getBody().getBusinessErrorCode());
    }
}
