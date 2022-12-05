/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.ds.server.service.DynamicSimulationService;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.http.MediaType.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + DynamicSimulationApi.API_VERSION)
@Tag(name = "Dynamic simulation server")
public class DynamicSimulationController {

    private final DynamicSimulationService dynamicSimulationService;

    public DynamicSimulationController(DynamicSimulationService dynamicSimulationService) {
        this.dynamicSimulationService = dynamicSimulationService;
    }

    @PostMapping(value = "/networks/{networkUuid}/run", produces = "application/json")
    @Operation(summary = "run the dynamic simulation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "")})
    public ResponseEntity<Mono<UUID>> run(@PathVariable("networkUuid") UUID networkUuid,
                                          @RequestParam(name = "variantId", required = false) String variantId,
                                          @DefaultValue("0") @RequestParam("startTime") int startTime,
                                          @RequestParam("stopTime") int stopTime,
                                          @RequestPart("dynamicModel") FilePart dynamicModel) {
        Mono<UUID> resultUuid = dynamicSimulationService.runAndSaveResult(networkUuid, variantId, startTime, stopTime, dynamicModel);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultUuid);
    }

    @GetMapping(value = "/results/{resultUuid}", produces = "application/json")
    @Operation(summary = "Get a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result has not been found")})
    public Mono<ResponseEntity<UUID>> getResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        Mono<UUID> result = dynamicSimulationService.getResult(resultUuid);
        return result.map(r -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/results/{resultUuid}/status", produces = "application/json")
    @Operation(summary = "Get the dynamic simulation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation status"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation status has not been found")})
    public Mono<ResponseEntity<String>> getStatus(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        Mono<String> result = dynamicSimulationService.getStatus(resultUuid);
        return result.map(r -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/results/{resultUuid}")
    @Operation(summary = "Delete a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result has been deleted")})
    public ResponseEntity<Mono<Void>> deleteResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        Mono<Void> result = dynamicSimulationService.deleteResult(resultUuid);
        return ResponseEntity.ok().body(result);
    }

    @DeleteMapping(value = "/results", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete all dynamic simulation results from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All dynamic simulation results have been deleted")})
    public ResponseEntity<Mono<Void>> deleteResults() {
        Mono<Void> result = dynamicSimulationService.deleteResults();
        return ResponseEntity.ok().body(result);
    }

}
