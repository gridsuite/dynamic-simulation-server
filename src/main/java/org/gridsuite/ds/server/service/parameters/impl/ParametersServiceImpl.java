/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters.impl;

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
import com.powsybl.ws.commons.computation.dto.ReportInfos;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.XmlSerializableParameter;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.dynamicmapping.Rule;
import org.gridsuite.ds.server.dto.dynamicmapping.automata.Automaton;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.network.NetworkInfos;
import org.gridsuite.ds.server.dto.solver.SolverInfos;
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
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.gridsuite.ds.server.DynamicSimulationException.Type.*;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersServiceImpl implements ParametersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersServiceImpl.class);
    public static final String FIELD_STATIC_ID = "staticId";

    private final CurveGroovyGeneratorService curveGroovyGeneratorService;

    private final String defaultProvider;

    @Autowired
    public ParametersServiceImpl(CurveGroovyGeneratorService curveGroovyGeneratorService,
                                 @Value("${dynamic-simulation.default-provider}") String defaultProvider) {
        this.curveGroovyGeneratorService = curveGroovyGeneratorService;
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

    @Override
    public DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams, String provider, DynamicSimulationParametersInfos inputParameters) {
        try {
            DynamicSimulationParameters parameters = new DynamicSimulationParameters();

            // TODO: Powsybl side - create an explicit dependency to Dynawo class and keep dynamic simulation abstraction all over this micro service
            if (DynawoSimulationProvider.NAME.equals(provider)) {
                // --- MODEL PAR --- //
                List<ParametersSet> modelsParameters = !ArrayUtils.isEmpty(dynamicParams) ? ParametersXml.load(new ByteArrayInputStream(dynamicParams)) : List.of();

                DynawoSimulationParameters dynawoParameters = new DynawoSimulationParameters();
                dynawoParameters.setModelsParameters(modelsParameters);
                parameters.addExtension(DynawoSimulationParameters.class, dynawoParameters);

                // --- SOLVER PAR --- //
                // solver from input parameter
                SolverInfos inputSolver = inputParameters.getSolvers().stream().filter(elem -> elem.getId().equals(inputParameters.getSolverId())).findFirst().orElse(null);
                if (inputSolver != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, inputSolver);
                    ParametersSet solverParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), inputSolver.getId());
                    dynawoParameters.setSolverType(inputSolver.getType().toSolverType());
                    dynawoParameters.setSolverParameters(solverParameters);
                }

                // --- NETWORK PAR --- //
                // network from input parameters
                NetworkInfos network = inputParameters.getNetwork();
                if (network != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, network);
                    ParametersSet networkParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), network.getId());
                    dynawoParameters.setNetworkParameters(networkParameters);
                }
            }

            return parameters;
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
    }

    @Override
    public DynamicSimulationRunContext createRunContext(UUID networkUuid, String variantId, String receiver, String provider, String mapping,
                                                        ReportInfos reportInfos, String userId, DynamicSimulationParametersInfos parameters) {
        DynamicSimulationRunContext runContext = DynamicSimulationRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .reportInfos(reportInfos)
                .userId(userId)
                .parameters(parameters)
                .build();

        // set provider for run context
        String providerToUse = provider;
        if (providerToUse == null) {
            providerToUse = runContext.getParameters().getProvider();
        }
        if (providerToUse == null) {
            providerToUse = defaultProvider;
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
                        "Only last rule can have empty filter: type " + equipmentType + ", rule index " + (rules.indexOf(ruleWithEmptyFilter) + 1));
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

                List<Identifiable<?>> matchedEquipmentsOfCurrentRule = FiltersUtils.getIdentifiables(filter, network, null);

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
}
