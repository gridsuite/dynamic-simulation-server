/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.ds.server.DynamicSimulationApi;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersValues;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;


/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + DynamicSimulationApi.API_VERSION + "/parameters")
@Tag(name = "Dynamic simulation server - Parameters")
public class DynamicSimulationParametersController {

    private final ParametersService parametersService;

    public DynamicSimulationParametersController(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @PostMapping(value = "/values", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the dynamic simulation parameters values")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation parameters values"),
        @ApiResponse(responseCode = "404", description = "The dynamic simulation parameters has not been found")})
    public ResponseEntity<DynamicSimulationParametersValues> getParametersValues(@RequestParam(name = "networkUuid") UUID networkUuid,
                                                                                    @RequestParam(name = "variantId", required = false) String variantId,
                                                                                    @RequestBody DynamicSimulationParametersInfos parameters) {
        DynamicSimulationParametersValues parametersValues = parametersService.getParametersValues(parameters, networkUuid, variantId);
        return ResponseEntity.of(Optional.ofNullable(parametersValues));
    }
}
