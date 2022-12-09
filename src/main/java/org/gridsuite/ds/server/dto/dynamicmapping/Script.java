/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.dto.dynamicmapping;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Data
@Schema(description = "Script")
@NoArgsConstructor
@AllArgsConstructor
public class Script {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Name of the parent mapping")
    private String parentName;

    @Schema(description = "Generated Script")
    private String script;

    @JsonIgnore
    @Schema(description = "Creation date")
    private Date createdDate;

    @Schema(description = "Script parameters are up to date")
    private boolean current;

    @Schema(description = "Parameter file")
    private String parametersFile;

}
