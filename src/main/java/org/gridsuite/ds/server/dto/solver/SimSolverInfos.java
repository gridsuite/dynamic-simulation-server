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

    public static final String FNORMTOL_ALG = "fnormtolAlg";
    public static final String INITIALADDTOL_ALG = "initialaddtolAlg";
    public static final String SCSTEPTOL_ALG = "scsteptolAlg";
    public static final String MXNEWTSTEP_ALG = "mxnewtstepAlg";
    public static final String MSBSET_ALG = "msbsetAlg";
    public static final String MXITER_ALG = "mxiterAlg";
    public static final String PRINTFL_ALG = "printflAlg";
    public static final String FNORMTOL_ALG_J = "fnormtolAlgJ";
    public static final String INITIALADDTOL_ALG_J = "initialaddtolAlgJ";
    public static final String SCSTEPTOL_ALG_J = "scsteptolAlgJ";
    public static final String MXNEWTSTEP_ALG_J = "mxnewtstepAlgJ";
    public static final String MSBSET_ALG_J = "msbsetAlgJ";
    public static final String MXITER_ALG_J = "mxiterAlgJ";
    public static final String PRINTFL_ALG_J = "printflAlgJ";
    public static final String FNORMTOL_ALG_INIT = "fnormtolAlgInit";
    public static final String INITIALADDTOL_ALG_INIT = "initialaddtolAlgInit";
    public static final String SCSTEPTOL_ALG_INIT = "scsteptolAlgInit";
    public static final String MXNEWTSTEP_ALG_INIT = "mxnewtstepAlgInit";
    public static final String MSBSET_ALG_INIT = "msbsetAlgInit";
    public static final String MXITER_ALG_INIT = "mxiterAlgInit";
    public static final String PRINTFL_ALG_INIT = "printflAlgInit";
    public static final String MAXIMUM_NUMBER_SLOW_STEP_INCREASE = "maximumNumberSlowStepIncrease";
    public static final String MINIMAL_ACCEPTABLE_STEP = "minimalAcceptableStep";

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

    private double fnormtolAlg;

    private double initialaddtolAlg;

    private double scsteptolAlg;

    private double mxnewtstepAlg;

    private int msbsetAlg;

    private int mxiterAlg;

    private int printflAlg;

    private double fnormtolAlgJ;

    private double initialaddtolAlgJ;

    private double scsteptolAlgJ;

    private double mxnewtstepAlgJ;

    private int msbsetAlgJ;

    private int mxiterAlgJ;

    private int printflAlgJ;

    private double fnormtolAlgInit;

    private double initialaddtolAlgInit;

    private double scsteptolAlgInit;

    private double mxnewtstepAlgInit;

    private int msbsetAlgInit;

    private int mxiterAlgInit;

    private int printflAlgInit;

    private int maximumNumberSlowStepIncrease;

    private double minimalAcceptableStep;

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
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, FNORMTOL_ALG, Double.toString(fnormtolAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INITIALADDTOL_ALG, Double.toString(initialaddtolAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, SCSTEPTOL_ALG, Double.toString(scsteptolAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MXNEWTSTEP_ALG, Double.toString(mxnewtstepAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MSBSET_ALG, Integer.toString(msbsetAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MXITER_ALG, Integer.toString(mxiterAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, PRINTFL_ALG, Integer.toString(printflAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, FNORMTOL_ALG_J, Double.toString(fnormtolAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INITIALADDTOL_ALG_J, Double.toString(initialaddtolAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, SCSTEPTOL_ALG_J, Double.toString(scsteptolAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MXNEWTSTEP_ALG_J, Double.toString(mxnewtstepAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MSBSET_ALG_J, Integer.toString(msbsetAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MXITER_ALG_J, Integer.toString(mxiterAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, PRINTFL_ALG_J, Integer.toString(printflAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, FNORMTOL_ALG_INIT, Double.toString(fnormtolAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INITIALADDTOL_ALG_INIT, Double.toString(initialaddtolAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, SCSTEPTOL_ALG_INIT, Double.toString(scsteptolAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MXNEWTSTEP_ALG_INIT, Double.toString(mxnewtstepAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MSBSET_ALG_INIT, Integer.toString(msbsetAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MXITER_ALG_INIT, Integer.toString(mxiterAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, PRINTFL_ALG_INIT, Integer.toString(printflAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MAXIMUM_NUMBER_SLOW_STEP_INCREASE, Integer.toString(maximumNumberSlowStepIncrease));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MINIMAL_ACCEPTABLE_STEP, Double.toString(minimalAcceptableStep));

        writer.writeEndElement();
    }
}
