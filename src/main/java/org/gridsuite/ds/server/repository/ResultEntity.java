/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.repository;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;

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
public class ResultEntity extends AbstractManuallyAssignedIdentifierEntity<UUID> implements Serializable {

    public ResultEntity(UUID id, Boolean result, String status) {
        this.id = id;
        this.result = result;
        this.status = status;
    }

    @Id
    @Column(name = "resultUuid")
    @GeneratedValue
    private UUID id;

    @Column(name = "result")
    private Boolean result;

    @Column(name = "status")
    private String status;

}
