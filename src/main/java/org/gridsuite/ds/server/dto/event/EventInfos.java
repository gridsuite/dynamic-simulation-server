/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EventInfos {

    @JsonProperty("uuid")
    @JsonIgnore
    private UUID id;

    @JsonIgnore
    private UUID nodeId;

    @JsonIgnore
    private String equipmentId;

    @JsonIgnore
    private String equipmentType;

    private String eventType;

    private List<EventPropertyInfos> properties = new ArrayList<>();

}
