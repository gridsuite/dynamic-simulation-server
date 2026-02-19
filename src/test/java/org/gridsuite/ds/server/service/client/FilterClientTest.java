/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.gridsuite.ds.server.service.client.utils.UrlUtils;
import org.gridsuite.ds.server.utils.assertions.Assertions;
import org.gridsuite.filter.AbstractFilter;
import org.gridsuite.filter.expertfilter.ExpertFilter;
import org.gridsuite.filter.expertfilter.expertrule.CombinatorExpertRule;
import org.gridsuite.filter.expertfilter.expertrule.NumberExpertRule;
import org.gridsuite.filter.expertfilter.expertrule.StringExpertRule;
import org.gridsuite.filter.utils.EquipmentType;
import org.gridsuite.filter.utils.expertfilter.CombinatorType;
import org.gridsuite.filter.utils.expertfilter.FieldType;
import org.gridsuite.filter.utils.expertfilter.OperatorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
class FilterClientTest extends AbstractWireMockRestClientTest {

    private FilterClient filterClient;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    @Override
    public void setup() {
        filterClient = new FilterClient(
            // use new WireMockServer(ACTIONS_PORT) to test with local server if needed
            initMockWebServer(new WireMockServer(wireMockConfig().dynamicPort())),
            restTemplate,
            objectMapper
        );
    }

    @Test
    void testGetFilters() throws JsonProcessingException {
        // --- Setup --- //
        String url = UrlUtils.buildEndPointUrl("", FilterClient.API_VERSION,
                FilterClient.FILTERS_GET_ENDPOINT);
        // prepare filters to return
        UUID expertFilterId1 = UUID.fromString("ecc0b070-1f24-4c7e-892e-65e2f994b7b0");
        ExpertFilter expertFilter1 = new ExpertFilter(expertFilterId1, null, EquipmentType.LOAD, CombinatorExpertRule.builder()
                .combinator(CombinatorType.AND)
                .rules(List.of(StringExpertRule.builder().field(FieldType.ID).operator(OperatorType.IS).value("load1").build()))
                .build());

        UUID expertFilterId2 = UUID.fromString("4c9bd0e7-9da5-442a-bd37-8989082f9389");
        ExpertFilter expertFilter2 = new ExpertFilter(expertFilterId2, null, EquipmentType.GENERATOR, CombinatorExpertRule.builder()
                .combinator(CombinatorType.OR)
                .rules(List.of(NumberExpertRule.builder().field(FieldType.NOMINAL_VOLTAGE).operator(OperatorType.GREATER).value(225d).build()))
                .build());

        List<AbstractFilter> returnedFilters = List.of(expertFilter1, expertFilter2);

        // stub method to return filters
        String responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(returnedFilters);
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(url))
                .withQueryParam("ids", WireMock.equalTo(expertFilterId1.toString()))
                .withQueryParam("ids", WireMock.equalTo(expertFilterId2.toString()))
                .willReturn(WireMock.ok()
                        .withHeader("Content-Type", APPLICATION_JSON_VALUE)
                        .withBody(responseBody))
        );

        // --- Invoke method to test --- //
        List<AbstractFilter> resultFilters = filterClient.getFilters(List.of(expertFilterId1, expertFilterId2));

        // --- Verify result --- //
        Assertions.assertThat(resultFilters).isEqualTo(returnedFilters);
        wireMockServer.verify(1,
            WireMock.getRequestedFor(WireMock.urlPathEqualTo(url))
                .withQueryParam("ids", WireMock.equalTo(expertFilterId1.toString()))
                .withQueryParam("ids", WireMock.equalTo(expertFilterId2.toString()))
        );
    }
}
