/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.ds.server.service.parameters;

import com.powsybl.dynamicsimulation.DynamicSimulationParameters;
import com.powsybl.dynawo.suppliers.dynamicmodels.DynamicModelConfig;
import com.powsybl.dynawo.suppliers.events.EventModelConfig;
import com.powsybl.iidm.network.Network;
import org.gridsuite.computation.dto.ReportInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersInfos;
import org.gridsuite.ds.server.dto.DynamicSimulationParametersValues;
import org.gridsuite.ds.server.dto.curve.CurveInfos;
import org.gridsuite.ds.server.dto.dynamicmapping.InputMapping;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.service.contexts.DynamicSimulationRunContext;

import java.util.List;
import java.util.UUID;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
public interface ParametersService {

    List<EventModelConfig> getEventModel(List<EventInfos> events);

    String getCurveModel(List<CurveInfos> curves);

    DynamicSimulationParameters getDynamicSimulationParameters(byte[] dynamicParams, String provider, DynamicSimulationParametersInfos inputParameters);

    DynamicSimulationRunContext createRunContext(UUID networkUuid, String variantId, String receiver, ReportInfos reportContext,
                                                 String userId, UUID parametersUuid, List<EventInfos> events, boolean debug);

    List<DynamicModelConfig> getDynamicModel(InputMapping inputMapping, Network network);

    List<DynamicModelConfig> getDynamicModel(String mappingName, UUID networkUuid, String variantId);

    DynamicSimulationParametersValues getParametersValues(UUID parametersUuid, UUID networkUuid, String variantId);

    // --- Dynamic simulation parameters related CRUD methods --- //

    List<DynamicSimulationParametersInfos> getAllParameters();

    DynamicSimulationParametersInfos getParameters(UUID parametersUuid);

    String getProvider(UUID parametersUuid);

    UUID createParameters(DynamicSimulationParametersInfos parametersInfos);

    UUID createDefaultParameters();

    UUID duplicateParameters(UUID sourceParametersUuid);

    void updateParameters(UUID parametersUuid, DynamicSimulationParametersInfos parametersInfos);

    void deleteParameters(UUID parametersUuid);
}
