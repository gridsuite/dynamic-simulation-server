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
import com.powsybl.commons.config.PlatformConfig;
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
import org.gridsuite.ds.server.service.DynamicSimulationResultContext;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public class DynamicSimulationIEEE14Test extends AbstractDynamicSimulationTest {
    private static final String NETWORK_UUID_STRING = "11111111-0000-0000-0000-000000000000";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "22222222-0000-0000-0000-000000000000";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    // directories
    private static final String DATA_IEEE14_BASE_DIR = "/data/ieee14";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    private static final String TEST_ENV_BASE_DIR = "/work/unittests";
    public static final String MODELS_PAR = "models.par";
    public static final String NETWORK_PAR = "network.par";
    public static final String SOLVERS_PAR = "solvers.par";
    public static final String MODELS_GROOVY = "models.groovy";
    public static final String EVENTS_GROOVY = "events.groovy";
    public static final String CURVES_GROOVY = "curves.groovy";
    public static final String PARAMETERS_JSON = "parameters.json";
    public static final String RESULT_JSON = "result.json";

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
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
    }

    private void loadFilesToConfig(String testBaseDir) throws IOException {
        Path configDir = PlatformConfig.defaultConfig().getConfigDir().orElseThrow();
        Path testDir = Files.createDirectories(configDir.getFileSystem().getPath(TEST_ENV_BASE_DIR, testBaseDir).toAbsolutePath());

        // copy model.par into test file system
        Files.copy(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, MODELS_PAR).toString()),
                testDir.resolve(MODELS_PAR));

        // copy network.par into test file system
        Files.copy(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, NETWORK_PAR).toString()),
                testDir.resolve(NETWORK_PAR));

        // copy solvers.par into test file system
        Files.copy(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, SOLVERS_PAR).toString()),
                testDir.resolve(SOLVERS_PAR));
    }

    private void configFilesToService(String testBaseDir) throws IOException {

        // load event model file
        byte[] eventBytes = StreamUtils.copyToByteArray(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, EVENTS_GROOVY).toString()));
        when(dynamicSimulationService.getEventModelContent()).thenReturn(eventBytes);

        // load curve file
        byte[] curveBytes = StreamUtils.copyToByteArray(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, CURVES_GROOVY).toString()));
        when(dynamicSimulationService.getCurveContent()).thenReturn(curveBytes);

        // load parameter file
        DynamicSimulationParameters parameters = JsonDynamicSimulationParameters.read(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, PARAMETERS_JSON).toString()));
        DynaWaltzParameters dynaWaltzParameters = parameters.getExtension(DynaWaltzParameters.class);

        String modelsDesPath = Paths.get(TEST_ENV_BASE_DIR, testBaseDir, MODELS_PAR).toString();
        dynaWaltzParameters.setParametersFile(modelsDesPath);

        String networkDesPath = Paths.get(TEST_ENV_BASE_DIR, testBaseDir, NETWORK_PAR).toString();
        dynaWaltzParameters.getNetwork().setParametersFile(networkDesPath);

        String solversDesPath = Paths.get(TEST_ENV_BASE_DIR, testBaseDir, SOLVERS_PAR).toString();
        dynaWaltzParameters.getSolver().setParametersFile(solversDesPath);
        when(dynamicSimulationService.getDynamicSimulationParameters()).thenReturn(parameters);
    }

    private String getResult(InputStream resultIS) throws IOException {
        DynamicSimulationResult result = DynamicSimulationResultDeserializer.read(resultIS);
        ByteArrayOutputStream bytesOS = new ByteArrayOutputStream();
        DynamicSimulationResultSerializer.write(result, bytesOS);
        String resultJson = bytesOS.toString();
        return resultJson;
    }

    @Test
    public void test01() throws IOException {
        String testBaseDir = "_01";
        // load dynamic model file
        ClassPathResource dynamicModel = new ClassPathResource(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, INPUT, MODELS_GROOVY).toString());
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("dynamicModel", dynamicModel)
                .filename(MODELS_GROOVY);

        // load inputs *.par files into test file system
        loadFilesToConfig(testBaseDir);
        // config input *.groovy, *.json files to services
        configFilesToService(testBaseDir);

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
        assertEquals(runUuid, UUID.fromString(messageSwitch.getHeaders().get(DynamicSimulationResultContext.RESULT_UUID).toString()));

        // prepare expected result to compare
        String jsonExpectedResult = getResult(getClass().getResourceAsStream(Paths.get(DATA_IEEE14_BASE_DIR, testBaseDir, OUTPUT, RESULT_JSON).toString()));

        // get the result from the message's payload
        ByteArrayInputStream bytesIS = new ByteArrayInputStream(messageSwitch.getPayload());
        String jsonResult = getResult(bytesIS);

        // compare result
        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(jsonExpectedResult), mapper.readTree(jsonResult));
    }
}
