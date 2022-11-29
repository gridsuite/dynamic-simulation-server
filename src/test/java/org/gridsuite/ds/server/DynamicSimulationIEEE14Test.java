/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationResult;
import com.powsybl.dynamicsimulation.json.DynamicSimulationResultDeserializer;
import com.powsybl.dynamicsimulation.json.JsonDynamicSimulationParameters;
import com.powsybl.dynawaltz.DynaWaltzParameters;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.ds.server.service.DynamicSimulationService;
import org.gridsuite.ds.server.service.DynamicSimulationWorkerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient
@EnableWebFlux
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class},
        initializers = CustomApplicationContextInitializer.class)
public class DynamicSimulationIEEE14Test {
    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "22222222-0000-0000-0000-000000000000";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String TEST_FILE = "IEEE14.iidm";

    private static final String DATA_IEEE14_BASE_DIR = "data/ieee14";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";


    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    @MockBean
    private NetworkStoreService networkStoreClient;

    @MockBean
    private DynamicSimulationService dynamicSimulationService;

    @SpyBean
    private DynamicSimulationWorkerService dynamicSimulationWorkerService;

    private FileSystem fileSystem;


    @Before
    public void init() throws IOException {
        //initialize in memory FS
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dynamicSimulationWorkerService.setFileSystem(fileSystem);

        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, TEST_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());

    }

    @Test
    public void test_01() throws IOException {
        String testBaseDir = "_01";
        String inputBaseDir = Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT).toString();
        String outputBaseDir = Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, OUTPUT).toString();

        // load dynamic model file
        ClassPathResource dynamicModel = new ClassPathResource(Paths.get(inputBaseDir, "models.groovy").toString());
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("dynamicModel", dynamicModel)
                .filename("models.groovy");

        // load event model file
        ClassPathResource eventModel = new ClassPathResource(Paths.get(inputBaseDir, "events.groovy").toString());
        byte[] eventBytes = StreamUtils.copyToByteArray(eventModel.getInputStream());
        given(dynamicSimulationService.getEventModelContent()).willReturn(eventBytes);

        // load curve file
        ClassPathResource curveModel = new ClassPathResource(Paths.get(inputBaseDir, "curve.groovy").toString());
        byte[] curveBytes = StreamUtils.copyToByteArray(curveModel.getInputStream());
        given(dynamicSimulationService.getCurveContent()).willReturn(curveBytes);

        // load parameter file
        ClassPathResource parametersModel = new ClassPathResource(Paths.get(inputBaseDir, "parameters.json").toString());
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(Path.of(parametersModel.getPath()));
        DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);
        // models.par path
        ClassPathResource dynamicParModel = new ClassPathResource(Paths.get(inputBaseDir, "models.par").toString());
        dynaWaltzParameters.setParametersFile(dynamicParModel.getPath());
        // network.par path
        ClassPathResource networkParModel = new ClassPathResource(Paths.get(inputBaseDir, "network.par").toString());
        dynaWaltzParameters.getNetwork().setParametersFile(networkParModel.getPath());
        // solvers.par path
        ClassPathResource solverParModel = new ClassPathResource(Paths.get(inputBaseDir, "solvers.par").toString());
        dynaWaltzParameters.getSolver().setParametersFile(solverParModel.getPath());
        given(dynamicSimulationService.getDynamicSimulationParameters()).willReturn(parameters);

        //run the dynamic simulation on a specific variant
        EntityExchangeResult<UUID> entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?variantId=" + VARIANT_1_ID + "&startTime=0&stopTime=50", NETWORK_UUID_STRING)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        UUID runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        Message<byte[]> messageSwitch = output.receive(1000, "ds.result.destination");
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get("resultUuid").toString()));

        // prepare result to compare
        ClassPathResource result = new ClassPathResource(Paths.get(outputBaseDir, "result.json").toString());
        DynamicSimulationResult expectedResult = DynamicSimulationResultDeserializer.read(Path.of(result.getPath()));

        // check the result to expected


    }

}
