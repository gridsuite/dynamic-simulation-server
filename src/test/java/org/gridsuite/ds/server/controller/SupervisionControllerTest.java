/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.junit.Assert.assertEquals;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
@RunWith(SpringRunner.class)
@AutoConfigureWebTestClient
@SpringBootTest
public class SupervisionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testResultCount() {
        //get the result timeline uuid of the calculation
        EntityExchangeResult<Integer> entityExchangeResult = webTestClient.get()
            .uri("/v1/supervision/results-count")
            .exchange()
            .expectStatus().isOk()
            .expectBody(Integer.class).returnResult();

        int resultCount = entityExchangeResult.getResponseBody();
        assertEquals(resultCount, 0);

    }

}
