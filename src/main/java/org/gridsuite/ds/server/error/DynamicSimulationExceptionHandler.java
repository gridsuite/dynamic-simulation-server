/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessExceptionHandler;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import com.powsybl.ws.commons.error.ServerNameProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@ControllerAdvice
public class DynamicSimulationExceptionHandler extends AbstractBusinessExceptionHandler<DynamicSimulationException, DynamicSimulationBusinessErrorCode> {

    protected DynamicSimulationExceptionHandler(ServerNameProvider serverNameProvider) {
        super(serverNameProvider);
    }

    @Override
    protected @NonNull DynamicSimulationBusinessErrorCode getBusinessCode(DynamicSimulationException e) {
        return e.getBusinessErrorCode();
    }

    protected HttpStatus mapStatus(DynamicSimulationBusinessErrorCode businessErrorCode) {
        return switch (businessErrorCode) {
            case PROVIDER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case MAPPING_NOT_PROVIDED,
                 MAPPING_NOT_LAST_RULE_WITH_EMPTY_FILTER_ERROR -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(DynamicSimulationException.class)
    public ResponseEntity<PowsyblWsProblemDetail> handleDynamicSimulationException(DynamicSimulationException exception, HttpServletRequest request) {
        return super.handleDomainException(exception, request);
    }
}
