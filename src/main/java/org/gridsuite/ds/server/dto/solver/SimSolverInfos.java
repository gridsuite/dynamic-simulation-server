/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.solver;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.gridsuite.ds.server.dto.XmlSerializableParameter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public class SimSolverInfos extends AbstractSolverInfos {

    public static final String H_MIN = "hMin";
    public static final String H_MAX = "hMax";
    public static final String K_REDUCE_STEP = "kReduceStep";
    public static final String N_EFF = "nEff";
    public static final String N_DEADBAND = "nDeadband";
    public static final String MAX_ROOT_RESTART = "maxRootRestart";
    public static final String MAX_NEWTON_TRY = "maxNewtonTry";
    public static final String LINEAR_SOLVER_NAME = "linearSolverName";
    public static final String RECALCULATE_STEP = "recalculateStep";


    // Important note: must using @JsonProperty to precise property's name when serialize/deserialize
    // fields which begin by a minuscule following by a majuscule, for example 'hMxxx', otherwise jackson
    // mapper will serialize as 'hmxxx' by default
    @JsonProperty(H_MIN)
    private double hMin;

    @JsonProperty(H_MAX)
    private double hMax;

    @JsonProperty(K_REDUCE_STEP)
    private double kReduceStep;

    @JsonProperty(N_EFF)

    private int nEff;

    @JsonProperty(N_DEADBAND)
    private int nDeadband;

    private int maxRootRestart;

    private int maxNewtonTry;

    private String linearSolverName;

    private boolean recalculateStep;

    @Override
    public void writeParameter(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(DYN_BASE_URI, "set");
        writer.writeAttribute("id", getId());

        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, H_MIN, Double.toString(hMin));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, H_MAX, Double.toString(hMax));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, K_REDUCE_STEP, Double.toString(kReduceStep));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, N_EFF, Integer.toString(nEff));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, N_DEADBAND, Integer.toString(nDeadband));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MAX_ROOT_RESTART, Integer.toString(maxRootRestart));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MAX_NEWTON_TRY, Integer.toString(maxNewtonTry));
        XmlSerializableParameter.writeParameter(writer, ParameterType.STRING, LINEAR_SOLVER_NAME, linearSolverName);
        XmlSerializableParameter.writeParameter(writer, ParameterType.BOOL, RECALCULATE_STEP, Boolean.toString(recalculateStep));

        writer.writeEndElement();
    }
}
