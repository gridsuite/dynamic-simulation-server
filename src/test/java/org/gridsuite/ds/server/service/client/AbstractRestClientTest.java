/*
 * Copyright (c) 2022-2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;
import org.gridsuite.ds.server.DynamicSimulationApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@SpringBootTest
@ContextHierarchy({@ContextConfiguration(classes = {DynamicSimulationApplication.class, TestChannelBinderConfiguration.class})})
abstract class AbstractRestClientTest {

    protected WebClient.Builder webClientBuilder;

    protected MockWebServer server;

    @Autowired
    ObjectMapper objectMapper;

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected abstract Dispatcher getDispatcher();

    ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected String initMockWebServer() {
        server = new MockWebServer();
        try {
            server.start();

            logger.info("Mock server started at port = " + server.getPort());
        } catch (IOException e) {
            throw new UncheckedIOException("Can not init the mock server " + this.getClass().getSimpleName(), e);
        }

        // setup dispatcher
        Dispatcher dispatcher = getDispatcher();
        // attach dispatcher
        server.setDispatcher(dispatcher);

        // get base URL
        HttpUrl baseUrl = server.url("");
        return baseUrl.toString();
    }

    @BeforeEach
    void setUp() {
        webClientBuilder = WebClient.builder();
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));

                }).build();
        webClientBuilder.exchangeStrategies(strategies);
    }

    @AfterEach
    void tearDown() {
        try {
            server.shutdown();
        } catch (Exception e) {
            logger.info("Can not shutdown the mock server " + this.getClass().getSimpleName());
        }
    }
}
