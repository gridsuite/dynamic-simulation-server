# Dynamic Simulation Server

[![Actions Status](https://github.com/gridsuite/dynamic-simulation-server/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/gridsuite/dynamic-simulation-server/actions)
[![Coverage Status](https://sonarcloud.io/api/project_badges/measure?project=org.gridsuite%3Adynamic-simulation-server&metric=coverage)](https://sonarcloud.io/component_measures?id=org.gridsuite%3Adynamic-simulation-server&metric=coverage)
[![MPL-2.0 License](https://img.shields.io/badge/license-MPL_2.0-blue.svg)](https://www.mozilla.org/en-US/MPL/2.0/)

## Description

The **dynamic-simulation-server** is a microservice of the [GridSuite](https://github.com/gridsuite) platform dedicated to **dynamic simulation computation** (time-domain, dynamic-model based simulation).

Dynamic simulation builds a dynamic model of the network (based on a mapping resolved by the [dynamic-mapping-server](https://github.com/gridsuite/dynamic-mapping-server)), applies configurable events on top of it, and runs a time-domain simulation using a Dynawo-based provider. It is the entry point of the dynamic computation chain: its output (network output state, dynamic model, Dynawo parameters) is reused as input by the [dynamic-security-analysis-server](https://github.com/gridsuite/dynamic-security-analysis-server) and the [dynamic-margin-calculation-server](https://github.com/gridsuite/dynamic-margin-calculation-server).

An **event** is a perturbation applied at a given time during the simulation (e.g. disconnecting a line, tripping a generator). Each event targets a specific equipment and is defined by an event type (e.g. `Disconnect`) and a set of properties (e.g. the time at which it occurs). Events are provided per run request (not persisted as reusable parameter sets) and are converted into Dynawo event models, applied on top of the dynamic model resolved from the mapping.

It provides the following capabilities:

- **Run dynamic simulation computations** on a network, resolving the dynamic model from a mapping and applying a list of events, using configurable providers (Dynawo).
- **Manage parameter sets** (create, read, update, duplicate, delete) with solver, network and mapping-based parameter values.
- **Track computation status** and invalidate/stop running computations.
- **Expose computation results**: time series and timeline identifiers (stored via the timeseries server), and downloadable outputs (network output state, dynamic model, Dynawo parameters) used as inputs by downstream dynamic computations.
- **Delete results** individually or in bulk.
- **Download debug files** produced by the underlying dynamic simulation provider.
- **Export the resolved dynamic model** of a network, for a given mapping, without running a simulation.
- Run computations **asynchronously** (via a RabbitMQ message queue).


---

## Technical Stack

- Spring Boot (Web, Data JPA, Actuator, Cloud Stream)
- PostgreSQL
- Liquibase
- RabbitMQ via Spring Cloud Stream
- API documentation: OpenAPI / Swagger (`springdoc`)
- Micrometer / Prometheus
- [gridsuite-computation](https://github.com/gridsuite/computation)
- [powsybl-dynawo](https://github.com/powsybl/powsybl-dynawo) (Dynawo dynamic simulation provider)

---

## Development Scripts

Build Docker image

```shell
mvn install -DskipTests -Dpowsybl.docker.install
```

Please read [liquibase usage](https://github.com/powsybl/powsybl-parent/#liquibase-usage) for instructions to automatically generate changesets. After you generated a changeset do not forget to add it to git and in `src/main/resources/db/changelog/db.changelog-master.yaml`.

---

## Interactions with Other Microservices

```text
┌───────────────────────────┐
│ dynamic-simulation-server │──► network-store-server   (read network topology)
│                           │──► dynamic-mapping-server  (resolve dynamic model mapping and export parameters)
│                           │──► filter-server           (resolve equipment lists used in events/curves)
│                           │──► timeseries-server       (store computed time series and timeline)
│                           │──► report-server           (post computation functional logs)
└───────────────────────────┘
          ▲  ▼
       RabbitMQ (ds.run / ds.cancel / ds.result / ds.stopped / ds.cancelfailed / ds.debug)
```

---

## Asynchronous Execution Flow

1. The controller publishes a message on the `ds.run` queue (`dsGroup` consumer group, load-balanced).
2. The dynamic model is resolved from the mapping (dynamic-mapping-server) and events are applied on top of it.
3. The computation result (time series, timeline, output state) is published on `ds.result`.
4. Cancellation of a running computation goes through the `ds.cancel` queue.
5. Debug information (when debug mode is enabled) is published on `ds.debug`.
6. Dead-letter queues (`ds.run.dlx`) and quorum queues ensure reliability.

---

## Parameters

Dynamic simulation parameters include:

- **Provider** selection (Dynawo).
- **Solver parameters** (IDA or SIM solver).
- **Network parameters** (capacitor no-reclosing delay, static var compensator behavior, etc.).
- **Mapping** reference, used to resolve the dynamic model of the network.
- Start/stop times.

Parameter sets can be created, duplicated, updated, retrieved and deleted through the REST API.

---

## Built on gridsuite-computation

The following capabilities are provided by the gridsuite-computation shared library:

- asynchronous run/cancel pipeline,
- transactional result notifications,
- report integration,
- Micrometer observability.

The dynamic-simulation-server itself focuses on dynamic-simulation-specific logic (dynamic model resolution, event handling, curve generation, provider integration) and delegates the common computation infrastructure to this lib.

---

## Useful Links

You can find [information on Dynawo here](https://dynawo.github.io/).
