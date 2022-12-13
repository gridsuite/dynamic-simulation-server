/*
 * Copyright (c) 2021-2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.gridsuite.ds.server.DynamicSimulationApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI createOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dynamic Simulation Server API")
                        .description("This is the documentation of the Dynamic-Simulation REST API")
                        .version(DynamicSimulationApi.API_VERSION));
    }
}
