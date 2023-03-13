/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
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
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.service.DynamicSimulationService;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.gridsuite.ds.server.DynamicSimulationApi.API_VERSION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + API_VERSION)
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
                                          @RequestParam("mappingName") String mappingName,
                                          @RequestParam(name = "receiver", required = false) String receiver) {
        Mono<UUID> resultUuid = dynamicSimulationService.runAndSaveResult(receiver, networkUuid, variantId, startTime, stopTime, mappingName);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultUuid);
    }

    @GetMapping(value = "/results/{resultUuid}/timeseries", produces = "application/json")
    @Operation(summary = "Get a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result has not been found")})
    public Mono<ResponseEntity<UUID>> getTimeSeriesResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        Mono<UUID> result = dynamicSimulationService.getTimeSeriesId(resultUuid);
        return result.map(r -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/results/{resultUuid}/timeline", produces = "application/json")
    @Operation(summary = "Get a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result has not been found")})
    public Mono<ResponseEntity<UUID>> getTimeLineResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        Mono<UUID> result = dynamicSimulationService.getTimeLineId(resultUuid);
        return result.map(r -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/results/{resultUuid}/status", produces = "application/json")
    @Operation(summary = "Get the dynamic simulation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation status"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation status has not been found")})
    public Mono<ResponseEntity<DynamicSimulationStatus>> getStatus(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        Mono<DynamicSimulationStatus> result = dynamicSimulationService.getStatus(resultUuid);
        return result.map(r -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(r))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/results/invalidate-status", produces = "application/json")
    @Operation(summary = "Invalidate the dynamic simulation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result uuids have been invalidated"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result has not been found")})
    public Mono<ResponseEntity<List<UUID>>> invalidateStatus(@Parameter(description = "Result UUIDs") @RequestParam("resultUuid") List<UUID> resultUuids) {
        Mono<List<UUID>> result = dynamicSimulationService.updateStatus(resultUuids, DynamicSimulationStatus.NOT_DONE.name());
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

    @PutMapping(value = "/results/{resultUuid}/stop", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Stop a dynamic simulation computation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation has been stopped")})
    public ResponseEntity<Mono<Void>> stop(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid,
                                           @Parameter(description = "Result receiver") @RequestParam(name = "receiver", required = false) String receiver) {
        Mono<Void> result = dynamicSimulationService.stop(receiver, resultUuid);
        return ResponseEntity.ok().body(result);
    }
}
