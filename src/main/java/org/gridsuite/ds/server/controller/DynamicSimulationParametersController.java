/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

import java.util.List;
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

    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create parameters")
    @ApiResponse(responseCode = "200", description = "parameters were created")
    public ResponseEntity<UUID> createParameters(
            @RequestBody DynamicSimulationParametersInfos parametersInfos) {
        return ResponseEntity.ok(parametersService.createParameters(parametersInfos));
    }

    @PostMapping(value = "/default")
    @Operation(summary = "Create default parameters")
    @ApiResponse(responseCode = "200", description = "Default parameters were created")
    public ResponseEntity<UUID> createDefaultParameters() {
        return ResponseEntity.ok(parametersService.createDefaultParameters());
    }

    @PostMapping(value = "", params = "duplicateFrom", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Duplicate parameters")
    @ApiResponse(responseCode = "200", description = "parameters were duplicated")
    public ResponseEntity<UUID> duplicateParameters(
            @Parameter(description = "source parameters UUID") @RequestParam("duplicateFrom") UUID sourceParametersUuid) {
        return ResponseEntity.ok(parametersService.duplicateParameters(sourceParametersUuid));
    }

    @GetMapping(value = "/{uuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get parameters")
    @ApiResponse(responseCode = "200", description = "parameters were returned")
    @ApiResponse(responseCode = "404", description = "parameters were not found")
    public ResponseEntity<DynamicSimulationParametersInfos> getParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        return ResponseEntity.ok(parametersService.getParameters(parametersUuid));
    }

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all parameters")
    @ApiResponse(responseCode = "200", description = "The list of all parameters was returned")
    public ResponseEntity<List<DynamicSimulationParametersInfos>> getAllParameters() {
        return ResponseEntity.ok().body(parametersService.getAllParameters());
    }

    @PutMapping(value = "/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update parameters")
    @ApiResponse(responseCode = "200", description = "parameters were updated")
    public ResponseEntity<Void> updateParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid,
            @RequestBody(required = false) DynamicSimulationParametersInfos parametersInfos) {
        parametersService.updateParameters(parametersUuid, parametersInfos);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/{uuid}")
    @Operation(summary = "Delete parameters")
    @ApiResponse(responseCode = "200", description = "parameters were deleted")
    public ResponseEntity<Void> deleteParameters(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        parametersService.deleteParameters(parametersUuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{uuid}/provider", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get provider")
    @ApiResponse(responseCode = "200", description = "provider were returned")
    @ApiResponse(responseCode = "404", description = "provider were not found")
    public ResponseEntity<String> getProvider(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid) {
        return ResponseEntity.ok(parametersService.getProvider(parametersUuid));
    }

    @GetMapping(value = "/{uuid}/values", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the dynamic simulation parameters values")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation parameters values"),
        @ApiResponse(responseCode = "404", description = "The dynamic simulation parameters has not been found")})
    public ResponseEntity<DynamicSimulationParametersValues> getParametersValues(
            @Parameter(description = "parameters UUID") @PathVariable("uuid") UUID parametersUuid,
            @RequestParam(name = "networkUuid") UUID networkUuid,
            @RequestParam(name = "variantId", required = false) String variantId) {
        DynamicSimulationParametersValues parametersValues = parametersService.getParametersValues(parametersUuid, networkUuid, variantId);
        return ResponseEntity.of(Optional.ofNullable(parametersValues));
    }
}
