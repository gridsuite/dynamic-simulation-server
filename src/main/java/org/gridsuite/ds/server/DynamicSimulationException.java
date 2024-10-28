/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import lombok.Getter;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Getter
public class DynamicSimulationException extends RuntimeException {

    public enum Type {
        URI_SYNTAX,
        PROVIDER_NOT_FOUND,
        MAPPING_NOT_PROVIDED,
        RESULT_UUID_NOT_FOUND,
        DYNAMIC_MAPPING_NOT_FOUND,
        EXPORT_PARAMETERS_ERROR,
        GET_DYNAMIC_MAPPING_ERROR,
        CREATE_TIME_SERIES_ERROR,
        DELETE_TIME_SERIES_ERROR,
        MAPPING_NOT_LAST_RULE_WITH_EMPTY_FILTER_ERROR,
        DIRECTORY_NOT_FOUND,
        STORAGE_DIR_NOT_CREATED,
        DIRECTORY_ALREADY_EXISTS,
    }

    private final Type type;

    public DynamicSimulationException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public static DynamicSimulationException createStorageNotInitialized(Path storageRootDir) {
        Objects.requireNonNull(storageRootDir);
        return new DynamicSimulationException(Type.STORAGE_DIR_NOT_CREATED, "The storage is not initialized: " + storageRootDir);
    }

    public static DynamicSimulationException createDirectoryAreadyExists(Path directory) {
        Objects.requireNonNull(directory);
        return new DynamicSimulationException(Type.DIRECTORY_ALREADY_EXISTS, "A directory with the same name already exists: " + directory);
    }

    public static DynamicSimulationException createDirectoryNotFound(UUID uuid) {
        Objects.requireNonNull(uuid);
        return new DynamicSimulationException(Type.DIRECTORY_NOT_FOUND, "The directory with the following uuid doesn't exist: " + uuid);
    }
}
