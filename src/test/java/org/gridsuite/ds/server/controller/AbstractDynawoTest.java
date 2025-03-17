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
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public abstract class AbstractDynawoTest {
    private static final String JAVA_DYNAWO_VERSION = "3.0.0";

    private static final String DOCKER_IMAGE_ID = "powsybl/java-dynawo:" + JAVA_DYNAWO_VERSION;

    // TODO wait junit5 to use @TempDir
    //@TempDir
    //public final Path localDir;

    protected ComputationManager computationManager;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        Path dockerDir = Path.of("/home/powsybl");
        ComputationDockerConfig config = new ComputationDockerConfig()
                .setDockerImageId(DOCKER_IMAGE_ID);
        Path localDir = tempFolder.getRoot().toPath();
        computationManager = new DockerLocalComputationManager(localDir, dockerDir, config);
    }

    @After
    public void tearDown() throws Exception {
        computationManager.close();
    }
}
