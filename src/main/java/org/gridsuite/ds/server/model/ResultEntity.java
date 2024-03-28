/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.ds.server.dto.DynamicSimulationStatus;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
@Setter
@Table(name = "result")
@NoArgsConstructor
@Entity
public class ResultEntity implements Serializable {

    public ResultEntity(UUID id, UUID timeSeriesId, UUID timeLineId, DynamicSimulationStatus status) {
        this.id = id;
        this.timeSeriesId = timeSeriesId;
        this.timeLineId = timeLineId;
        this.status = status;
    }

    @Id
    @Column(name = "resultUuid")
    private UUID id;

    @Column(name = "timeSeriesUuid")
    private UUID timeSeriesId;

    @Column(name = "timeLineUuid")
    private UUID timeLineId;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private DynamicSimulationStatus status;

}
