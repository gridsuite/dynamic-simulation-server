/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

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
            case URI_SYNTAX,
                    CREATE_MAPPING_SCRIPT_ERROR,
                    CREATE_TIME_SERIES_ERROR,
                    DELETE_TIME_SERIES_ERROR
                    -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
        };
    }

}
