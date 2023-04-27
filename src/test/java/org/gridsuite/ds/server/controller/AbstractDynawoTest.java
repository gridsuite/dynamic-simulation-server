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
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public abstract class AbstractDynawoTest {
    private static final String DYNAWO_VERSION = "1.4.0";

    private static final String DOCKER_IMAGE_ID = "powsybl/java-dynawo:" + DYNAWO_VERSION;

    // TODO wait junit5 to use @TempDir
    private final Path localDir = Paths.get("/tmp");

    protected ComputationManager computationManager;

    @Before
    public void setUp() throws IOException {
        Path dockerDir = Path.of("/home/powsybl");
        ComputationDockerConfig config = new ComputationDockerConfig()
                .setDockerImageId(DOCKER_IMAGE_ID);
        computationManager = new DockerLocalComputationManager(localDir, dockerDir, config);
    }

    @After
    public void tearDown() {
        computationManager.close();
    }
}
