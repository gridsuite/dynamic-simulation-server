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
    public static final String MAX_NEWTON_TRY = "maxNewtonTry";
    public static final String LINEAR_SOLVER_NAME = "linearSolverName";
    public static final String FNORMTOL = "fnormtol";
    public static final String INITIALADDTOL = "initialaddtol";
    public static final String SCSTEPTOL = "scsteptol";
    public static final String MXNEWTSTEP = "mxnewtstep";
    public static final String MSBSET = "msbset";
    public static final String MXITER = "mxiter";
    public static final String PRINTFL = "printfl";
    public static final String OPTIMIZE_ALGEBRAIC_RESIDUALS_EVALUATIONS = "optimizeAlgebraicResidualsEvaluations";
    public static final String SKIP_NR_IF_INITIAL_GUESS_OK = "skipNRIfInitialGuessOK";
    public static final String ENABLE_SILENT_Z = "enableSilentZ";
    public static final String OPTIMIZE_REINIT_ALGEBRAIC_RESIDUALS_EVALUATIONS = "optimizeReinitAlgebraicResidualsEvaluations";
    public static final String MINIMUM_MODE_CHANGE_TYPE_FOR_ALGEBRAIC_RESTORATION = "minimumModeChangeTypeForAlgebraicRestoration";
    public static final String MINIMUM_MODE_CHANGE_TYPE_FOR_ALGEBRAIC_RESTORATION_INIT = "minimumModeChangeTypeForAlgebraicRestorationInit";

    // Important note: must using @JsonProperty to precise property's name when serialize/deserialize
    // fields which begin by a minuscule following by a majuscule, for example 'hMxxx', otherwise jackson
    // mapper will serialize as 'hmxxx' by default
    @JsonProperty(H_MIN)
    private double hMin;

    @JsonProperty(H_MAX)
    private double hMax;

    @JsonProperty(K_REDUCE_STEP)
    private double kReduceStep;

    private int maxNewtonTry;

    private String linearSolverName;

    private double fnormtol;

    private double initialaddtol;

    private double scsteptol;

    private double mxnewtstep;

    private int msbset;

    private int mxiter;

    private int printfl;

    private boolean optimizeAlgebraicResidualsEvaluations;

    private boolean skipNRIfInitialGuessOK;

    private boolean enableSilentZ;

    private boolean optimizeReinitAlgebraicResidualsEvaluations;

    private String minimumModeChangeTypeForAlgebraicRestoration;

    private String minimumModeChangeTypeForAlgebraicRestorationInit;

    @Override
    public void writeParameter(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(DYN_BASE_URI, "set");
        writer.writeAttribute("id", getId());

        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, H_MIN, Double.toString(hMin));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, H_MAX, Double.toString(hMax));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, K_REDUCE_STEP, Double.toString(kReduceStep));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MAX_NEWTON_TRY, Integer.toString(maxNewtonTry));
        XmlSerializableParameter.writeParameter(writer, ParameterType.STRING, LINEAR_SOLVER_NAME, linearSolverName);
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, FNORMTOL, Double.toString(fnormtol));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INITIALADDTOL, Double.toString(initialaddtol));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, SCSTEPTOL, Double.toString(scsteptol));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MXNEWTSTEP, Double.toString(mxnewtstep));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MSBSET, Integer.toString(msbset));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MXITER, Integer.toString(mxiter));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, PRINTFL, Integer.toString(printfl));
        XmlSerializableParameter.writeParameter(writer, ParameterType.BOOL, OPTIMIZE_ALGEBRAIC_RESIDUALS_EVALUATIONS, Boolean.toString(optimizeAlgebraicResidualsEvaluations));
        XmlSerializableParameter.writeParameter(writer, ParameterType.BOOL, SKIP_NR_IF_INITIAL_GUESS_OK, Boolean.toString(skipNRIfInitialGuessOK));
        XmlSerializableParameter.writeParameter(writer, ParameterType.BOOL, ENABLE_SILENT_Z, Boolean.toString(enableSilentZ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.BOOL, OPTIMIZE_REINIT_ALGEBRAIC_RESIDUALS_EVALUATIONS, Boolean.toString(optimizeReinitAlgebraicResidualsEvaluations));
        XmlSerializableParameter.writeParameter(writer, ParameterType.STRING, MINIMUM_MODE_CHANGE_TYPE_FOR_ALGEBRAIC_RESTORATION, minimumModeChangeTypeForAlgebraicRestoration);
        XmlSerializableParameter.writeParameter(writer, ParameterType.STRING, MINIMUM_MODE_CHANGE_TYPE_FOR_ALGEBRAIC_RESTORATION_INIT, minimumModeChangeTypeForAlgebraicRestorationInit);

        super.writeParameter(writer);
        writer.writeEndElement();
    }
}
