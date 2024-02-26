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
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.service.DynamicSimulationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.gridsuite.ds.server.DynamicSimulationApi.API_VERSION;
import static org.gridsuite.ds.server.service.contexts.DynamicSimulationFailedContext.HEADER_USER_ID;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

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
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Run dynamic simulation")})
    public ResponseEntity<UUID> run(@PathVariable("networkUuid") UUID networkUuid,
                                          @RequestParam(name = "variantId", required = false) String variantId,
                                          @RequestParam(name = "receiver", required = false) String receiver,
                                          @RequestParam("mappingName") String mappingName,
                                          @RequestParam(name = "provider", required = false) String provider,
                                          @RequestBody DynamicSimulationParametersInfos parameters,
                                          @RequestHeader(HEADER_USER_ID) String userId) {
        UUID resultUuid = dynamicSimulationService.runAndSaveResult(receiver, networkUuid, variantId, mappingName, provider, parameters, userId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultUuid);
    }

    @GetMapping(value = "/results/{resultUuid}/timeseries", produces = "application/json")
    @Operation(summary = "Get a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation series uuid is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<UUID> getTimeSeriesResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        UUID result = dynamicSimulationService.getTimeSeriesId(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result) :
                ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/results/{resultUuid}/timeline", produces = "application/json")
    @Operation(summary = "Get a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation timeline uuid is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<UUID> getTimeLineResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        UUID result = dynamicSimulationService.getTimeLineId(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result) :
                ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/results/{resultUuid}/status", produces = "application/json")
    @Operation(summary = "Get the dynamic simulation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation status"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation status is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<DynamicSimulationStatus> getStatus(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        DynamicSimulationStatus result = dynamicSimulationService.getStatus(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result) :
                ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/results/invalidate-status", produces = "application/json")
    @Operation(summary = "Invalidate the dynamic simulation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result uuids have been invalidated"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result has not been found")})
    public ResponseEntity<List<UUID>> invalidateStatus(@Parameter(description = "Result UUIDs") @RequestParam("resultUuid") List<UUID> resultUuids) {
        List<UUID> result = dynamicSimulationService.updateStatus(resultUuids, DynamicSimulationStatus.NOT_DONE.name());
        return CollectionUtils.isEmpty(result) ? ResponseEntity.notFound().build() :
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    @DeleteMapping(value = "/results/{resultUuid}")
    @Operation(summary = "Delete a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result has been deleted")})
    public ResponseEntity<Void> deleteResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        dynamicSimulationService.deleteResult(resultUuid);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/results", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete all dynamic simulation results from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All dynamic simulation results have been deleted")})
    public ResponseEntity<Void> deleteResults() {
        dynamicSimulationService.deleteResults();
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/results/{resultUuid}/stop", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Stop a dynamic simulation computation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation has been stopped")})
    public ResponseEntity<Void> stop(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid,
                                           @Parameter(description = "Result receiver") @RequestParam(name = "receiver", required = false) String receiver) {
        dynamicSimulationService.stop(receiver, resultUuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/providers", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all dynamic simulation providers")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic simulation providers have been found")})
    public ResponseEntity<List<String>> getProviders() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(dynamicSimulationService.getProviders());
    }

    @GetMapping(value = "/default-provider", produces = TEXT_PLAIN_VALUE)
    @Operation(summary = "Get dynamic simulation default provider")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The dynamic simulation default provider has been found"))
    public ResponseEntity<String> getDefaultProvider() {
        return ResponseEntity.ok().body(dynamicSimulationService.getDefaultProvider());
    }
}
