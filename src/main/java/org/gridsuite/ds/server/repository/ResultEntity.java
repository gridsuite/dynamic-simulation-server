/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
@Setter
@Table("result")
@AllArgsConstructor
public class ResultEntity implements Serializable, Persistable<UUID> {

    @PersistenceConstructor
    public ResultEntity(UUID id, Boolean result, String status) {
        this.id = id;
        this.result = result;
        this.status = status;
        this.newElement = false;
    }

    @Id
    @Column("resultUuid")
    private UUID id;

    @Column("result")
    private Boolean result;

    @Column("status")
    private String status;

    @Transient
    private boolean newElement;

    @Override
    public boolean isNew() {
        if (newElement && id == null) {
            id = UUID.randomUUID();
        }
        return newElement;
    }
}
