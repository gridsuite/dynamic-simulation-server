/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters.impl;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.exceptions.UncheckedXmlStreamException;
import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynamicsimulation.DynamicSimulationProvider;
import com.powsybl.dynawo.DynawoSimulationParameters;
import com.powsybl.dynawo.DynawoSimulationProvider;
import com.powsybl.dynawo.parameters.ParametersSet;
import com.powsybl.dynawo.suppliers.PropertyBuilder;
import com.powsybl.dynawo.suppliers.PropertyType;
import com.powsybl.dynawo.suppliers.SetGroupType;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.events.EventModelConfig;
import com.powsybl.dynawo.xml.ParametersXml;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersValues;
import org.gridsuite.ds.server.dto.XmlSerializableParameter;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.ParameterFile;
import org.gridsuite.ds.server.dto.dynamicmapping.Rule;
import org.gridsuite.ds.server.dto.dynamicmapping.automata.Automaton;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.network.NetworkInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;
import org.gridsuite.ds.server.error.DynamicSimulationException;
import org.gridsuite.ds.server.service.client.FilterClient;
import org.gridsuite.ds.server.service.client.dynamicmapping.DynamicMappingClient;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;
import org.gridsuite.ds.server.service.parameters.CurveGroovyGeneratorService;
import org.gridsuite.ds.server.service.parameters.ParametersService;
import org.gridsuite.ds.server.utils.Utils;
import org.gridsuite.filter.expertfilter.ExpertFilter;
import org.gridsuite.filter.expertfilter.expertrule.CombinatorExpertRule;
import org.gridsuite.filter.utils.EquipmentType;
import org.gridsuite.filter.utils.FiltersUtils;
import org.gridsuite.filter.utils.expertfilter.CombinatorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.ds.server.error.DynamicSimulationBusinessErrorCode.*;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersServiceImpl implements ParametersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersServiceImpl.class);
    public static final String FIELD_STATIC_ID = "staticId";

    private final NetworkStoreService networkStoreService;
    private final CurveGroovyGeneratorService curveGroovyGeneratorService;
    private final DynamicMappingClient dynamicMappingClient;
    private final FilterClient filterClient;

    private final String defaultProvider;

    @Autowired
    public ParametersServiceImpl(NetworkStoreService networkStoreService,
                                 CurveGroovyGeneratorService curveGroovyGeneratorService,
                                 DynamicMappingClient dynamicMappingClient,
                                 FilterClient filterClient,
                                 @Value("${dynamic-simulation.default-provider}") String defaultProvider) {
        this.networkStoreService = networkStoreService;
        this.curveGroovyGeneratorService = curveGroovyGeneratorService;
        this.dynamicMappingClient = dynamicMappingClient;
        this.filterClient = filterClient;
        this.defaultProvider = defaultProvider;
    }

    @Override
    public List<EventModelConfig> getEventModel(List<EventInfos> events) {
        if (CollectionUtils.isEmpty(events)) {
            return Collections.emptyList();
        }

        return events.stream().map(event ->
                        new EventModelConfig(
                                event.getEventType(),
                                event.getProperties().stream().map(Utils::convertProperty).filter(Objects::nonNull).toList()))
                .toList();
    }

    @Override
    public String getCurveModel(List<CurveInfos> curves) {
        String generatedGroovyCurves = curveGroovyGeneratorService.generate(curves != null ? curves : Collections.emptyList());
        LOGGER.info(generatedGroovyCurves);
        return generatedGroovyCurves;
    }

    private DynawoSimulationParameters getDynawoSimulationParameters(byte[] dynamicParams, DynamicSimulationParametersInfos inputParameters) {
        try {
            DynawoSimulationParameters dynawoSimulationParameters = new DynawoSimulationParameters();
            // --- MODEL PAR --- //
            List<ParametersSet> modelsParameters = !ArrayUtils.isEmpty(dynamicParams) ? ParametersXml.load(new ByteArrayInputStream(dynamicParams)) : List.of();
            dynawoSimulationParameters.setModelsParameters(modelsParameters);

            // --- SOLVER PAR --- //
            // solver from input parameter
            SolverInfos inputSolver = inputParameters.getSolvers().stream().filter(elem -> elem.getId().equals(inputParameters.getSolverId())).findFirst().orElse(null);
            if (inputSolver != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, inputSolver);
                ParametersSet solverParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), inputSolver.getId());
                dynawoSimulationParameters.setSolverType(inputSolver.getType().toSolverType());
                dynawoSimulationParameters.setSolverParameters(solverParameters);
            }

            // --- NETWORK PAR --- //
            // network from input parameters
            NetworkInfos network = inputParameters.getNetwork();
            if (network != null) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, network);
                ParametersSet networkParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), network.getId());
                dynawoSimulationParameters.setNetworkParameters(networkParameters);
            }

            return dynawoSimulationParameters;
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
    }

    @Override
    public DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams, String provider, DynamicSimulationParametersInfos inputParameters) {
        DynamicSimulationParameters parameters = new DynamicSimulationParameters();

        // TODO: Powsybl side - create an explicit dependency to Dynawo class and keep dynamic simulation abstraction all over this micro service
        if (DynawoSimulationProvider.NAME.equals(provider)) {
            DynawoSimulationParameters dynawoSimulationParameters = getDynawoSimulationParameters(dynamicParams, inputParameters);

            // TODO : a bug in powsybl-dynawo while deserializing in dynamic security analysis server, TO REMOVE
            Set<DynawoSimulationParameters.SpecificLog> specificLogs = EnumSet.of(DynawoSimulationParameters.SpecificLog.NETWORK);
            dynawoSimulationParameters.setSpecificLogs(specificLogs);

            parameters.addExtension(DynawoSimulationParameters.class, dynawoSimulationParameters);
        }
        return parameters;
    }

    @Override
    public DynamicSimulationRunContext createRunContext(UUID networkUuid, String variantId, String receiver, String provider, String mapping,
                                                        ReportInfos reportInfos, String userId, DynamicSimulationParametersInfos parameters, boolean debug) {
        DynamicSimulationRunContext runContext = DynamicSimulationRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .reportInfos(reportInfos)
                .userId(userId)
                .parameters(parameters)
                .debug(debug)
                .build();

        // set provider for run context
        String providerToUse = provider;
        if (providerToUse == null) {
            providerToUse = Optional.ofNullable(runContext.getParameters().getProvider()).orElse(defaultProvider);
        }

        runContext.setProvider(providerToUse);

        // check provider
        if (DynamicSimulationProvider.findAll().stream()
                .noneMatch(elem -> Objects.equals(elem.getName(), runContext.getProvider()))) {
            throw new DynamicSimulationException(PROVIDER_NOT_FOUND, "Dynamic simulation provider not found: " + runContext.getProvider());
        }

        // set mapping for run context
        String mappingToUse = mapping;
        if (mappingToUse == null) {
            mappingToUse = runContext.getParameters().getMapping();
        }
        runContext.setMapping(mappingToUse);

        // check mapping
        if (runContext.getMapping() == null) {
            throw new DynamicSimulationException(MAPPING_NOT_PROVIDED, "Dynamic simulation mapping not provided");
        }

        return runContext;
    }

    @Override
    public List<DynamicModelConfig> getDynamicModel(InputMapping inputMapping, Network network) {
        if (inputMapping == null) {
            return Collections.emptyList();
        }

        List<DynamicModelConfig> dynamicModel = new ArrayList<>();

        // --- transform equipment rules to DynamicModelConfigs --- //
        List<Rule> allRules = inputMapping.rules();
        // grouping rules by equipment type
        Map<EquipmentType, List<Rule>> rulesByEquipmentTypeMap = allRules.stream().collect(Collectors.groupingBy(Rule::equipmentType));

        // Only last rule can have empty filter checking
        rulesByEquipmentTypeMap.forEach((equipmentType, rules) -> {
            Rule ruleWithEmptyFilter = rules.stream().filter(rule -> rule.filter() == null).findFirst().orElse(null);
            if (ruleWithEmptyFilter != null && rules.indexOf(ruleWithEmptyFilter) != (rules.size() - 1)) {
                throw new DynamicSimulationException(MAPPING_NOT_LAST_RULE_WITH_EMPTY_FILTER_ERROR,
                        "Only last rule can have empty filter", Map.of("equipmentType", equipmentType, "index", rules.indexOf(ruleWithEmptyFilter) + 1));
            }
        });

        // performing transformation
        rulesByEquipmentTypeMap.forEach((equipmentType, rules) -> {
            // accumulate matched equipment ids to compute otherwise case (last rule without filters)
            Set<String> matchedEquipmentIdsOfCurrentType = new TreeSet<>();

            dynamicModel.addAll(rules.stream().flatMap(rule -> {
                ExpertFilter filter = rule.filter();

                // otherwise case, create an expert filter with AND operator and empty rules to get all equipments of the same type
                if (filter == null) {
                    filter = ExpertFilter.builder()
                            .equipmentType(equipmentType)
                            .rules(CombinatorExpertRule.builder().combinator(CombinatorType.AND).rules(List.of()).build())
                            .build();
                }

                List<Identifiable<?>> matchedEquipmentsOfCurrentRule = FiltersUtils.getIdentifiables(filter, network, filterClient::getFilters);

                // eliminate already matched equipments to avoid duplication
                if (!matchedEquipmentIdsOfCurrentType.isEmpty()) {
                    matchedEquipmentsOfCurrentRule = matchedEquipmentsOfCurrentRule.stream().filter(elem -> !matchedEquipmentIdsOfCurrentType.contains(elem.getId())).toList();
                }

                matchedEquipmentIdsOfCurrentType.addAll(matchedEquipmentsOfCurrentRule.stream().map(Identifiable::getId).toList());

                return matchedEquipmentsOfCurrentRule.stream().map(equipment -> new DynamicModelConfig(
                        rule.mappedModel(),
                        rule.setGroup(),
                        SetGroupType.valueOf(rule.groupType().name()),
                        List.of(new PropertyBuilder()
                                .name(FIELD_STATIC_ID)
                                .value(equipment.getId())
                                .type(PropertyType.STRING)
                                .build())));
            }).toList());
        });

        // transform automatons to DynamicModelConfigs
        List<Automaton> automata = inputMapping.automata();
        dynamicModel.addAll(automata.stream().map(automaton ->
                new DynamicModelConfig(
                        automaton.model(),
                        automaton.setGroup(),
                        automaton.properties().stream().map(Utils::convertProperty).filter(Objects::nonNull).toList())
        ).toList());

        return dynamicModel;
    }

    private DynamicSimulationParametersValues getParametersValues(DynamicSimulationParametersInfos parametersInfos, Network network) {
        // get parameters file from dynamic mapping server
        ParameterFile parameterFile = dynamicMappingClient.exportParameters(parametersInfos.getMapping());

        // get dynawo simulation parameters
        String parameterFileContent = parameterFile.fileContent();
        DynawoSimulationParameters dynawoSimulationParameters = getDynawoSimulationParameters(
                parameterFileContent.getBytes(StandardCharsets.UTF_8), parametersInfos);

        // get mapping then generate dynamic model configs
        InputMapping inputMapping = dynamicMappingClient.getMapping(parametersInfos.getMapping());
        List<DynamicModelConfig> dynamicModel = getDynamicModel(inputMapping, network);

        return new DynamicSimulationParametersValues(dynamicModel, dynawoSimulationParameters);
    }

    private Network getNetwork(UUID networkUuid, String variantId) {
        try {
            Network network = networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
            String variant = StringUtils.isBlank(variantId) ? VariantManagerConstants.INITIAL_VARIANT_ID : variantId;
            network.getVariantManager().setWorkingVariant(variant);
            return network;
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public List<DynamicModelConfig> getDynamicModel(String mappingName, UUID networkUuid, String variantId) {
        InputMapping inputMapping = dynamicMappingClient.getMapping(mappingName);
        Network network = getNetwork(networkUuid, variantId);

        return getDynamicModel(inputMapping, network);
    }

    @Override
    public DynamicSimulationParametersValues getParametersValues(DynamicSimulationParametersInfos parametersInfos, UUID networkUuid, String variantId) {
        Network network = getNetwork(networkUuid, variantId);

        return getParametersValues(parametersInfos, network);
    }
}
