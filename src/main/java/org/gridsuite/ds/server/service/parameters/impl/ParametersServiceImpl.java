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
import com.powsybl.dynawaltz.DynaWaltzParameters;
import com.powsybl.dynawaltz.DynaWaltzProvider;
import com.powsybl.dynawaltz.parameters.ParametersSet;
import com.powsybl.dynawaltz.rte.mapping.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawaltz.rte.mapping.dynamicmodels.PropertyBuilder;
import com.powsybl.dynawaltz.rte.mapping.dynamicmodels.PropertyType;
import com.powsybl.dynawaltz.xml.ParametersXml;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.ArrayUtils;
import org.gridsuite.ds.server.DynamicSimulationException;
import org.gridsuite.ds.server.computation.utils.ReportContext;
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
import org.gridsuite.ds.server.service.parameters.EventGroovyGeneratorService;
import org.gridsuite.ds.server.service.parameters.ParametersService;
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

import static org.gridsuite.ds.server.DynamicSimulationException.Type.MAPPING_NOT_PROVIDED;
import static org.gridsuite.ds.server.DynamicSimulationException.Type.PROVIDER_NOT_FOUND;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class ParametersServiceImpl implements ParametersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametersServiceImpl.class);

    private final CurveGroovyGeneratorService curveGroovyGeneratorService;

    private final EventGroovyGeneratorService eventGroovyGeneratorService;

    private final String defaultProvider;

    @Autowired
    public ParametersServiceImpl(CurveGroovyGeneratorService curveGroovyGeneratorService,
                                 EventGroovyGeneratorService eventGroovyGeneratorService,
                                 @Value("${dynamic-simulation.default-provider}") String defaultProvider) {
        this.curveGroovyGeneratorService = curveGroovyGeneratorService;
        this.eventGroovyGeneratorService = eventGroovyGeneratorService;
        this.defaultProvider = defaultProvider;
    }

    @Override
    public String getEventModel(List<EventInfos> events) {
        String generatedGroovyEvents = eventGroovyGeneratorService.generate(events != null ? events : Collections.emptyList());
        LOGGER.info(generatedGroovyEvents);
        return generatedGroovyEvents;
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

            // TODO: Powsybl side - create an explicit dependency to DynaWaltz class and keep dynamic simulation abstraction all over this micro service
            if (DynaWaltzProvider.NAME.equals(provider)) {
                // --- MODEL PAR --- //
                List<ParametersSet> modelsParameters = !ArrayUtils.isEmpty(dynamicParams) ? ParametersXml.load(new ByteArrayInputStream(dynamicParams)) : List.of();

                DynaWaltzParameters dynaWaltzParameters = new DynaWaltzParameters();
                dynaWaltzParameters.setModelsParameters(modelsParameters);
                parameters.addExtension(DynaWaltzParameters.class, dynaWaltzParameters);

                // --- SOLVER PAR --- //
                // solver from input parameter
                SolverInfos inputSolver = inputParameters.getSolvers().stream().filter(elem -> elem.getId().equals(inputParameters.getSolverId())).findFirst().orElse(null);
                if (inputSolver != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, inputSolver);
                    ParametersSet solverParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), inputSolver.getId());
                    dynaWaltzParameters.setSolverType(inputSolver.getType().toSolverType());
                    dynaWaltzParameters.setSolverParameters(solverParameters);
                }

                // --- NETWORK PAR --- //
                // network from input parameters
                NetworkInfos network = inputParameters.getNetwork();
                if (network != null) {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    XmlSerializableParameter.writeParameter(os, XmlSerializableParameter.PARAMETER_SET, network);
                    ParametersSet networkParameters = ParametersXml.load(new ByteArrayInputStream(os.toByteArray()), network.getId());
                    dynaWaltzParameters.setNetworkParameters(networkParameters);
                }

                // Quick fix to make working in powsybl-dynawo 2.1.0
                // Cannot invoke "com.powsybl.dynawaltz.DumpFileParameters.useDumpFile()"
                // because "dumpFileParameters" is null
                // TODO This param must be configured by default during the creation at the powsybl level
                dynaWaltzParameters.setDefaultDumpFileParameters();
            }

            return parameters;
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
    }

    @Override
    public DynamicSimulationRunContext createRunContext(UUID networkUuid, String variantId, String receiver, String provider, String mapping,
                                                 ReportContext reportContext, String userId, DynamicSimulationParametersInfos parameters) {
        DynamicSimulationRunContext runContext = DynamicSimulationRunContext.builder()
                .networkUuid(networkUuid)
                .variantId(variantId)
                .receiver(receiver)
                .reportContext(reportContext)
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
        // performing transformation
        rulesByEquipmentTypeMap.forEach((equipmentType, rules) -> {
            // accumulate matched equipment ids to compute otherwise case (last rule without filters)
            Set<String> matchedEquipmentIds = new TreeSet<>();

            dynamicModel.addAll(rules.stream().flatMap(rule -> {
                ExpertFilter filter = rule.filter();

                // otherwise case, create an expert filter with AND operator and empty rules to get all equipments of the same type
                if (filter == null) {
                    filter = ExpertFilter.builder()
                            .equipmentType(equipmentType)
                            .rules(CombinatorExpertRule.builder().combinator(CombinatorType.AND).rules(List.of()).build())
                            .build();
                }

                List<Identifiable<?>> matchedEquipments = FiltersUtils.getIdentifiables(filter, network, null);

                // eliminate already matched equipments to avoid duplication
                if (!matchedEquipmentIds.isEmpty()) {
                    matchedEquipments = matchedEquipments.stream().filter(elem -> !matchedEquipmentIds.contains(elem.getId())).toList();
                }

                matchedEquipmentIds.addAll(matchedEquipments.stream().map(Identifiable::getId).toList());

                return matchedEquipments.stream().map(equipment -> new DynamicModelConfig(
                    rule.mappedModel(),
                    rule.setGroup(),
                    rule.groupType(),
                    List.of(new PropertyBuilder()
                        .name("staticId")
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
                automaton.properties().stream().map(property ->
                    new PropertyBuilder()
                        .name(property.name())
                        .value(property.value())
                        .type(property.type())
                        .build()
                ).toList())
        ).toList());

        return dynamicModel;
    }
}
