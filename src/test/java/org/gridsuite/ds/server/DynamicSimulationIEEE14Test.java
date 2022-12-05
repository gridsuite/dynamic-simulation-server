/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.gridsuite.ds.server.json.DynamicSimulationResultSerializer;
import org.gridsuite.ds.server.service.DynamicSimulationService;
import org.gridsuite.ds.server.service.DynamicSimulationWorkerService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StreamUtils;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationIEEE14Test extends AbstractDynamicSimulationTest {
    public static final String TEST_BASE_DIR = "_01";
    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "22222222-0000-0000-0000-000000000000";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    // directories
    private static final String DATA_IEEE14_BASE_DIR = "/data/ieee14";
    private static final String INPUT = "input";
    public static final String INPUT_BASE_DIR = Paths.get(DATA_IEEE14_BASE_DIR, TEST_BASE_DIR, INPUT).toString();
    private static final String OUTPUT = "output";
    public static final String OUTPUT_BASE_DIR = Paths.get(DATA_IEEE14_BASE_DIR, TEST_BASE_DIR, OUTPUT).toString();
    private static final String TEST_ENV_BASE_DIR = "/work/unittests";
    private static final String CONFIG_TEST_DIR = "/com/powsybl/config/test";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OutputDestination output;

    @Autowired
    private InputDestination input;

    @MockBean
    private NetworkStoreService networkStoreClient;

    @SpyBean
    private DynamicSimulationService dynamicSimulationService;

    @SpyBean
    private DynamicSimulationWorkerService dynamicSimulationWorkerService;

    private FileSystem fileSystem;

    @Before
    public void init() throws IOException {
        //initialize in memory FS
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        dynamicSimulationWorkerService.setFileSystem(fileSystem);
        loadFilesToConfig(); // Important: must call before import network
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
    }

    private void loadFilesToConfig() throws IOException {
        // get config.yml in order to get parent folder
        ClassPathResource configResource = new ClassPathResource(Paths.get(CONFIG_TEST_DIR, "config.yml").toString());
        File configFile = configResource.getFile();
        String configPathDir = configFile.getParent();

        // models.par path
        ClassPathResource dynamicParModel = new ClassPathResource(Paths.get(INPUT_BASE_DIR, "models.par").toString());
        Path modelsSrcPath = dynamicParModel.getFile().toPath();
        Path modelsConfigPath = Paths.get(configPathDir, "models.par");
        Files.copy(modelsSrcPath, modelsConfigPath, StandardCopyOption.REPLACE_EXISTING);

        // network.par path
        ClassPathResource networkParModel = new ClassPathResource(Paths.get(INPUT_BASE_DIR, "network.par").toString());
        Path networkSrcPath = networkParModel.getFile().toPath();
        Path networkConfigPath = Paths.get(configPathDir, "network.par");
        Files.copy(networkSrcPath, networkConfigPath, StandardCopyOption.REPLACE_EXISTING);

        // solvers.par path
        ClassPathResource solverParModel = new ClassPathResource(Paths.get(INPUT_BASE_DIR, "solvers.par").toString());
        Path solversSrcPath = solverParModel.getFile().toPath();
        Path solversConfigPath = Paths.get(configPathDir, "solvers.par");
        Files.copy(solversSrcPath, solversConfigPath, StandardCopyOption.REPLACE_EXISTING);

        // rewrite filelist
        ClassPathResource fileListResource = new ClassPathResource(Paths.get(CONFIG_TEST_DIR, "filelist.txt").toString());
        Path fileListSrcPath = fileListResource.getFile().toPath();
        String newFileListContent = "config.yml" +
                "\n" + "models.par" +
                "\n" + "network.par" +
                "\n" + "solvers.par";
        Files.writeString(fileListSrcPath, newFileListContent, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void test01() throws IOException {
        // load dynamic model file
        ClassPathResource dynamicModel = new ClassPathResource(Paths.get(INPUT_BASE_DIR, "models.groovy").toString());
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("dynamicModel", dynamicModel)
                .filename("models.groovy");

        // load event model file
        ClassPathResource eventModel = new ClassPathResource(Paths.get(INPUT_BASE_DIR, "events.groovy").toString());
        byte[] eventBytes = StreamUtils.copyToByteArray(eventModel.getInputStream());
        when(dynamicSimulationService.getEventModelContent()).thenReturn(eventBytes);

        // load curve file
        ClassPathResource curveModel = new ClassPathResource(Paths.get(INPUT_BASE_DIR, "curves.groovy").toString());
        byte[] curveBytes = StreamUtils.copyToByteArray(curveModel.getInputStream());
        when(dynamicSimulationService.getCurveContent()).thenReturn(curveBytes);

        // load parameter file
        ClassPathResource parametersModel = new ClassPathResource(Paths.get(INPUT_BASE_DIR, "parameters.json").toString());
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(parametersModel.getInputStream());
        DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);

        String modelsDesPath = Paths.get(TEST_ENV_BASE_DIR, "models.par").toString();
        dynaWaltzParameters.setParametersFile(modelsDesPath);

        String networkDesPath = Paths.get(TEST_ENV_BASE_DIR, "network.par").toString();
        dynaWaltzParameters.getNetwork().setParametersFile(networkDesPath);

        String solversDesPath = Paths.get(TEST_ENV_BASE_DIR, "solvers.par").toString();
        dynaWaltzParameters.getSolver().setParametersFile(solversDesPath);
        when(dynamicSimulationService.getDynamicSimulationParameters()).thenReturn(parameters);

        //run the dynamic simulation (on a specific variant with variantId=" + VARIANT_1_ID + ")
        EntityExchangeResult<UUID> entityExchangeResult = webTestClient.post()
                .uri("/v1/networks/{networkUuid}/run?&startTime=0&stopTime=50", NETWORK_UUID_STRING)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(UUID.class)
                .returnResult();

        UUID runUuid = UUID.fromString(entityExchangeResult.getResponseBody().toString());

        Message<byte[]> messageSwitch = output.receive(1000, "ds.result.destination");
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get("resultUuid").toString()));

        // prepare expected result to compare
        ClassPathResource expectedResultPathResource = new ClassPathResource(Paths.get(OUTPUT_BASE_DIR, "result.json").toString());
        DynamicSimulationResult expectedResult = DynamicSimulationResultDeserializer.read(expectedResultPathResource.getInputStream());
        ByteArrayOutputStream bytesOS = new ByteArrayOutputStream();
        DynamicSimulationResultSerializer.write(expectedResult, bytesOS);
        String jsonExpectedResult = bytesOS.toString();
        System.out.println("Expected");
        System.out.println(jsonExpectedResult);

        // get the result
        ByteArrayInputStream bytesIS = new ByteArrayInputStream(messageSwitch.getPayload());
        DynamicSimulationResult result = DynamicSimulationResultDeserializer.read(bytesIS);
        ByteArrayOutputStream bytesOSResult = new ByteArrayOutputStream();
        DynamicSimulationResultSerializer.write(result, bytesOSResult);
        String jsonResult = bytesOSResult.toString();
        System.out.println("Actual");
        System.out.println(jsonResult);

        // compare result
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(jsonExpectedResult), mapper.readTree(jsonResult));
    }
}
