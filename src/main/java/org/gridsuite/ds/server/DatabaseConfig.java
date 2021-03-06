/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.gridsuite.ds.server.repository.ResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */

@Configuration
@PropertySource(value = {"classpath:database.properties"})
@PropertySource(value = {"file:/config/database.properties"}, ignoreResourceNotFound = true)
@EnableR2dbcRepositories(basePackageClasses = {ResultRepository.class})
public class DatabaseConfig extends AbstractR2dbcConfiguration {

    @Autowired
    Environment env;

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        String driver = env.getRequiredProperty("driver");
        if (driver.equals("h2")) {
            return new H2ConnectionFactory(H2ConnectionConfiguration.builder()
                    .inMemory(env.getRequiredProperty("database"))
                    .option("DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                    .build());
        } else if (driver.equals("postgresql")) {
            return new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                    .host(env.getRequiredProperty("host"))
                    .port(Integer.parseInt(env.getRequiredProperty("port")))
                    .database(env.getRequiredProperty("database"))
                    .username(env.getRequiredProperty("login"))
                    .password(env.getRequiredProperty("password"))
                    .build());
        } else {
            throw new DynamicSimulationException(DynamicSimulationException.Type.DATABASE_DRIVER_NOT_FOUND);
        }
    }
}
