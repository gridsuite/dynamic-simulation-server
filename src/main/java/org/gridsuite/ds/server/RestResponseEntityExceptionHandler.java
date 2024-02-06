/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebInputException;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler {

    @ExceptionHandler(DynamicSimulationException.class)
    protected ResponseEntity<Object> handleDynamicSimulationException(DynamicSimulationException exception) {
        DynamicSimulationException.Type type = exception.getType();
        return switch (type) {
            case DYNAMIC_MAPPING_NOT_FOUND,
                    RESULT_UUID_NOT_FOUND
                    -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
            case URI_SYNTAX
                    -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        };
    }

    @ExceptionHandler(ServerWebInputException.class)
    protected ResponseEntity<Object> handleServerWebInputException(ServerWebInputException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof TypeMismatchException && cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return ResponseEntity.status(exception.getStatusCode()).body(cause.getMessage());
    }

    @ExceptionHandler(TypeMismatchException.class)
    protected ResponseEntity<Object> handleTypeMismatchException(TypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getCause().getMessage());
    }
}
