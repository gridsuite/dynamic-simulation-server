/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.datasource.ReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.iidm.network.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.gridsuite.ds.server.DynamicSimulationApplication;
import org.gridsuite.ds.server.controller.utils.ParameterUtils;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersValues;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.ParameterFile;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.utils.assertions.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.gridsuite.ds.server.utils.Utils.RESOURCE_PATH_DELIMITER;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@AutoConfigureMockMvc
@SpringBootTest
@ContextConfiguration(classes = {DynamicSimulationApplication.class})
public class DynamicSimulationParametersControllerIEEE14Test {
    // mapping names
    public static final String MAPPING_NAME_01 = "_01";

    // directories
    public static final String DATA_IEEE14_BASE_DIR = RESOURCE_PATH_DELIMITER + "data" + RESOURCE_PATH_DELIMITER + "ieee14";
    public static final String INPUT = "input";
    public static final String MAPPING_FILE = "mapping.json";
    public static final String MODELS_PAR = "models.par";

    private static final String NETWORK_UUID_STRING = "46661c4e-f948-475f-8be9-eed0c9d4da23";
    private static final String NETWORK_UUID_NOT_FOUND_STRING = "6ac83015-4e9b-48c0-be96-1b5b241fb725";
    private static final String VARIANT_1_ID = "variant_1";
    private static final String NETWORK_FILE = "IEEE14.iidm";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    protected DynamicMappingClient dynamicMappingClient;

    @MockitoBean
    protected NetworkStoreService networkStoreClient;

    private void initNetworkStoreServiceMock() {
        ReadOnlyDataSource dataSource = new ResourceDataSource("IEEE14",
                new ResourceSet(DATA_IEEE14_BASE_DIR, NETWORK_FILE));
        Network network = Importers.importData("XIIDM", dataSource, null);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_STRING), PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreClient.getNetwork(UUID.fromString(NETWORK_UUID_NOT_FOUND_STRING), PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
    }

    private void initDynamicMappingServiceMock() {
        try {
            String inputDir = DATA_IEEE14_BASE_DIR +
                              RESOURCE_PATH_DELIMITER + MAPPING_NAME_01 +
                              RESOURCE_PATH_DELIMITER + INPUT;

            // load models.par
            String parametersFilePath = inputDir + RESOURCE_PATH_DELIMITER + MODELS_PAR;
            InputStream parametersFileIS = getClass().getResourceAsStream(parametersFilePath);
            byte[] parametersFileBytes;
            parametersFileBytes = StreamUtils.copyToByteArray(parametersFileIS);
            String parametersFile = new String(parametersFileBytes, StandardCharsets.UTF_8);

            ParameterFile parameterFile = new ParameterFile(
                    MAPPING_NAME_01,
                    parametersFile);
            given(dynamicMappingClient.exportParameters(MAPPING_NAME_01)).willReturn(parameterFile);

            // load mapping.json
            String mappingPath = inputDir + RESOURCE_PATH_DELIMITER + MAPPING_FILE;
            InputMapping inputMapping = objectMapper.readValue(getClass().getResourceAsStream(mappingPath), InputMapping.class);
            given(dynamicMappingClient.getMapping(MAPPING_NAME_01)).willReturn(inputMapping);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @BeforeEach
    void setUp() {
        initNetworkStoreServiceMock();
        initDynamicMappingServiceMock();
    }

    @Test
    void testGetParametersValues() throws Exception {
        // --- Setup --- //
        DynamicSimulationParametersInfos parameters = ParameterUtils.getDefaultDynamicSimulationParameters();
        parameters.setSolverId("SIM");
        parameters.setMapping(MAPPING_NAME_01);

        String parametersJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parameters);

        // --- Execute --- //
        MvcResult result = mockMvc.perform(post("/v1/parameters/values")
                .param("networkUuid", NETWORK_UUID_STRING)
                .param("variantId", VARIANT_1_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(parametersJson))
                    .andExpect(status().isOk())
                    .andReturn();

        String resultParametersJson = result.getResponse().getContentAsString();
        DynamicSimulationParametersValues parametersValues = objectMapper.readValue(resultParametersJson, DynamicSimulationParametersValues.class);

        // --- Verify --- //
        Assertions.assertThat(parametersValues.getDynamicModel()).isNotEmpty();

        Assertions.assertThat(parametersValues.getDynawoParameters().getModelParameters()).isNotEmpty();
        Assertions.assertThat(parametersValues.getDynawoParameters().getSolverParameters().getParameters()).isNotEmpty();
        Assertions.assertThat(parametersValues.getDynawoParameters().getNetworkParameters().getParameters()).isNotEmpty();

        verify(dynamicMappingClient, times(1)).getMapping(MAPPING_NAME_01);
        verify(dynamicMappingClient, times(1)).exportParameters(MAPPING_NAME_01);
    }
}
