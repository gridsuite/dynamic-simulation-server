/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessException;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;

import java.util.Map;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
public class DynamicSimulationException extends AbstractBusinessException {

    private final DynamicSimulationBusinessErrorCode errorCode;
    private final transient Map<String, Object> businessErrorValues;

    @NonNull
    @Override
    public DynamicSimulationBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }

    @NotNull
    @Override
    public Map<String, Object> getBusinessErrorValues() {
        return businessErrorValues;
    }

    public DynamicSimulationException(DynamicSimulationBusinessErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public DynamicSimulationException(DynamicSimulationBusinessErrorCode errorCode, String message, Map<String, Object> businessErrorValues) {
        super(message);
        this.errorCode = errorCode;
        this.businessErrorValues = businessErrorValues != null ? Map.copyOf(businessErrorValues) : Map.of();
    }
}
