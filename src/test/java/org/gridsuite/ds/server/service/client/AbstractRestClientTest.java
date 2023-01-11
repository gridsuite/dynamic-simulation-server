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
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@RunWith(SpringRunner.class)
public abstract class AbstractRestClientTest {

    protected WebClient.Builder webClientBuilder;

    protected MockWebServer server;

    protected final ObjectMapper objectMapper = createObjectMapper();

    public final Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    protected abstract Dispatcher getDispatcher();

    public static ObjectMapper createObjectMapper() {
        var objectMapper = Jackson2ObjectMapperBuilder.json().build();
        return objectMapper;
    }

    protected String initMockWebServer(int port) throws RuntimeException {
        server = new MockWebServer();
        try {
            server.start(port);
        } catch (IOException e) {
            throw new RuntimeException("Can not init the mock server " + this.getClass().getSimpleName(), e);
        }

        // setup dispatcher
        Dispatcher dispatcher = getDispatcher();
        // attach dispatcher
        server.setDispatcher(dispatcher);

        // get base URL
        HttpUrl baseUrl = server.url("");
        return baseUrl.toString();
    }

    @Before
    public void setUp() throws IOException {
        webClientBuilder = WebClient.builder();
        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));

                }).build();
        webClientBuilder.exchangeStrategies(strategies);
    }

    @After
    public void tearDown() {
        try {
            server.shutdown();
        } catch (Exception e) {
            getLogger().info("Can not shutdown the mock server " + this.getClass().getSimpleName());
        }
    }
}
