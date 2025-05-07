/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import com.powsybl.ws.commons.computation.dto.ReportInfos;
import com.powsybl.ws.commons.computation.utils.StreamerWithInfos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;
import org.gridsuite.ds.server.service.DynamicSimulationResultService;
import org.gridsuite.ds.server.service.DynamicSimulationService;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static com.powsybl.ws.commons.computation.service.NotificationService.HEADER_USER_ID;
import static org.gridsuite.ds.server.DynamicSimulationApi.API_VERSION;
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
    private final DynamicSimulationResultService dynamicSimulationResultService;
    private final ParametersService parametersService;

    public DynamicSimulationController(DynamicSimulationService dynamicSimulationService,
                                       DynamicSimulationResultService dynamicSimulationResultService,
                                       ParametersService parametersService) {
        this.dynamicSimulationService = dynamicSimulationService;
        this.dynamicSimulationResultService = dynamicSimulationResultService;
        this.parametersService = parametersService;
    }

    @PostMapping(value = "/networks/{networkUuid}/run", produces = "application/json")
    @Operation(summary = "run the dynamic simulation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Run dynamic simulation")})
    public ResponseEntity<UUID> run(@PathVariable("networkUuid") UUID networkUuid,
                                          @RequestParam(name = "variantId", required = false) String variantId,
                                          @RequestParam(name = "receiver", required = false) String receiver,
                                          @RequestParam(name = "mappingName", required = false) String mappingName,
                                          @RequestParam(name = "reportUuid", required = false) UUID reportId,
                                          @RequestParam(name = "reporterId", required = false) String reportName,
                                          @RequestParam(name = "reportType", required = false, defaultValue = "DynamicSimulation") String reportType,
                                          @RequestParam(name = "provider", required = false) String provider,
                                          @RequestParam(name = "debug", required = false, defaultValue = "false") boolean debug,
                                          @RequestBody DynamicSimulationParametersInfos parameters,
                                          @RequestHeader(HEADER_USER_ID) String userId) {

        DynamicSimulationRunContext dynamicSimulationRunContext = parametersService.createRunContext(
            networkUuid,
            variantId,
            receiver,
            provider,
            mappingName,
            ReportInfos.builder().reportUuid(reportId).reporterId(reportName).computationType(reportType).build(),
            userId,
            parameters,
            debug);

        UUID resultUuid = dynamicSimulationService.runAndSaveResult(dynamicSimulationRunContext);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultUuid);
    }

    @GetMapping(value = "/results/{resultUuid}/timeseries", produces = "application/json")
    @Operation(summary = "Get a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation series uuid is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<UUID> getTimeSeriesResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        UUID result = dynamicSimulationResultService.getTimeSeriesId(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result) :
                ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/results/{resultUuid}/timeline", produces = "application/json")
    @Operation(summary = "Get a dynamic simulation result from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation timeline uuid is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<UUID> getTimeLineResult(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        UUID result = dynamicSimulationResultService.getTimeLineId(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result) :
                ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/results/{resultUuid}/status", produces = "application/json")
    @Operation(summary = "Get the dynamic simulation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation status"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation status is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<DynamicSimulationStatus> getStatus(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        DynamicSimulationStatus result = dynamicSimulationResultService.findStatus(resultUuid);
        return ResponseEntity.ok().body(result);
    }

    @GetMapping(value = "/results/{resultUuid}/output-state", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get the dynamic simulation output state in gzip format from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation output state"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation output state is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<byte[]> getOutputState(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        byte[] result = dynamicSimulationResultService.getOutputState(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(result) :
                ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/results/{resultUuid}/dynamic-model", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get the dynamic simulation dynamic model in gzip format from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation dynamic model"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation dynamic model is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<byte[]> getDynamicModel(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        byte[] result = dynamicSimulationResultService.getDynamicModel(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(result) :
                ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/results/{resultUuid}/parameters", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "Get the dynamic simulation parameters in gzip format from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation parameters"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation parameters is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<byte[]> getParameters(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        byte[] result = dynamicSimulationResultService.getParameters(resultUuid);
        return result != null ? ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(result) :
                ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/results/invalidate-status", produces = "application/json")
    @Operation(summary = "Invalidate the dynamic simulation status from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation result uuids have been invalidated"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result has not been found")})
    public ResponseEntity<List<UUID>> invalidateStatus(@Parameter(description = "Result UUIDs") @RequestParam("resultUuid") List<UUID> resultUuids) {
        List<UUID> result = dynamicSimulationResultService.updateStatus(resultUuids, DynamicSimulationStatus.NOT_DONE);
        return CollectionUtils.isEmpty(result) ? ResponseEntity.notFound().build() :
                ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(result);
    }

    @DeleteMapping(value = "/results", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Delete dynamic simulation results from the database")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Dynamic simulation results have been deleted")})
    public ResponseEntity<Void> deleteResults(@Parameter(description = "Results UUID") @RequestParam(value = "resultsUuids", required = false) List<UUID> resultsUuids) {
        dynamicSimulationService.deleteResults(resultsUuids);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/results/{resultUuid}/stop", produces = APPLICATION_JSON_VALUE)
    @Operation(summary = "Stop a dynamic simulation computation")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation has been stopped")})
    public ResponseEntity<Void> stop(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid,
                                           @Parameter(description = "Result receiver") @RequestParam(name = "receiver", required = false, defaultValue = "") String receiver) {
        dynamicSimulationService.stop(resultUuid, receiver);
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

    @GetMapping(value = "/results/{resultUuid}/download/debug-file", produces = "application/json")
    @Operation(summary = "Get the dynamic simulation debug file stream")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The dynamic simulation debug file stream"),
        @ApiResponse(responseCode = "204", description = "Dynamic simulation debug file is empty"),
        @ApiResponse(responseCode = "404", description = "Dynamic simulation result uuid has not been found")})
    public ResponseEntity<StreamingResponseBody> getDebugFileStream(@Parameter(description = "Result UUID") @PathVariable("resultUuid") UUID resultUuid) {
        try {
            StreamerWithInfos fileStreamerWithInfos = dynamicSimulationService.getDebugFileStreamer(resultUuid);
            StreamingResponseBody streamer = outputStream -> fileStreamerWithInfos.getStreamer().accept(outputStream);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileStreamerWithInfos.getFileName()).build());
            headers.setContentLength(fileStreamerWithInfos.getFileLength());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(streamer);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
