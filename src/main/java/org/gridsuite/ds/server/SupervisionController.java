/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.ds.server.service.DynamicSimulationService;
import org.gridsuite.ds.server.service.SupervisionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + DynamicSimulationApi.API_VERSION + "/supervision")
@Tag(name = "Dynamic simulation server - Supervision")
public class SupervisionController {
    private final SupervisionService supervisionService;
    private final DynamicSimulationService dynamicSimulationService;

    public SupervisionController(SupervisionService supervisionService, DynamicSimulationService dynamicSimulationService) {
        this.supervisionService = supervisionService;
        this.dynamicSimulationService = dynamicSimulationService;
    }

    @GetMapping(value = "/results-count")
    @Operation(summary = "Get results count")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The count of all results")})
    public ResponseEntity<Integer> getResultsCount() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(supervisionService.getResultsCount());
    }

    @DeleteMapping(value = "/results")
    @Operation(summary = "Delete all dynamic simulation results from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All dynamic simulation results have been deleted")})
    public ResponseEntity<Void> deleteAllResults() {
        dynamicSimulationService.deleteAllResults();
        return ResponseEntity.ok().build();
    }
}
