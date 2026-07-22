/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.controller;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.test.ComputationDockerConfig;
import com.powsybl.computation.local.test.DockerLocalComputationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
abstract class AbstractDynawoTest {
    private static final String JAVA_DYNAWO_VERSION = "3.1.0";

    private static final String DOCKER_IMAGE_ID = "powsybl/java-dynawo:" + JAVA_DYNAWO_VERSION;

    @TempDir
    Path tempDir;

    protected ComputationManager computationManager;

    @BeforeEach
    void setUp() throws IOException {
        Path dockerDir = Path.of("/home/powsybl");
        ComputationDockerConfig config = new ComputationDockerConfig()
                .setDockerImageId(DOCKER_IMAGE_ID);
        computationManager = new DockerLocalComputationManager(tempDir, dockerDir, config);
    }

    @AfterEach
    void tearDown() throws Exception {
        // Exception may be thrown by child classes overriding this method
        computationManager.close();
    }
}
