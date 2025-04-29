package org.gridsuite.ds.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(name = "s3.enabled", havingValue = "true")
@Import({com.powsybl.ws.commons.computation.S3Config.class})
public class S3AutoImportConfig {
}
