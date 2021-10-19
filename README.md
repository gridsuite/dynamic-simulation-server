[![Actions Status](https://github.com/gridsuite/dynamic-simulation-server/workflows/CI/badge.svg)](https://github.com/gridsuite/dynamic-simulation-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Adynamic-simulation-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Adynamic-simulation-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)
# dynamic-simulation-server

```
mvn package -DskipTests && rm -f src/main/resources/result.sql && java  -jar target/gridsuite-dynamic-simulation-server-1.0.0-SNAPSHOT-exec.jar --spring.jpa.properties.javax.persistence.schema-generation.scripts.action=create
```
