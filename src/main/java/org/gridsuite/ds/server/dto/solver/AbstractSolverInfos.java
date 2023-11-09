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
public abstract class AbstractSolverInfos implements SolverInfos {

    public static final String F_NORM_TOL_ALG = "fnormtolAlg";
    public static final String INITIAL_ADD_TOL_ALG = "initialaddtolAlg";
    public static final String SC_STEP_TOL_ALG = "scsteptolAlg";
    public static final String MX_NEW_T_STEP_ALG = "mxnewtstepAlg";
    public static final String MSBSET_ALG = "msbsetAlg";
    public static final String MX_ITER_ALG = "mxiterAlg";
    public static final String PRINT_FL_ALG = "printflAlg";
    public static final String F_NORM_TOL_ALG_J = "fnormtolAlgJ";
    public static final String INITIAL_ADD_TOL_ALG_J = "initialaddtolAlgJ";
    public static final String SC_STEP_TOL_ALG_J = "scsteptolAlgJ";
    public static final String MX_NEWT_STEP_ALG_J = "mxnewtstepAlgJ";
    public static final String MSBSET_ALG_J = "msbsetAlgJ";
    public static final String MX_ITER_ALG_J = "mxiterAlgJ";
    public static final String PRINT_FL_ALG_J = "printflAlgJ";
    public static final String F_NORM_TOL_ALG_INIT = "fnormtolAlgInit";
    public static final String INITIAL_ADD_TOL_ALG_INIT = "initialaddtolAlgInit";
    public static final String SC_STEP_TOL_ALG_INIT = "scsteptolAlgInit";
    public static final String MX_NEW_T_STEP_ALG_INIT = "mxnewtstepAlgInit";
    public static final String MSBSET_ALG_INIT = "msbsetAlgInit";
    public static final String MX_ITER_ALG_INIT = "mxiterAlgInit";
    public static final String PRINT_FL_ALG_INIT = "printflAlgInit";
    public static final String MAXIMUM_NUMBER_SLOW_STEP_INCREASE = "maximumNumberSlowStepIncrease";
    public static final String MINIMAL_ACCEPTABLE_STEP = "minimalAcceptableStep";

    private String id;
    private SolverTypeInfos type;

    // Important note: must using @JsonProperty to precise property's name when serialize/deserialize
    // fields which begin by a minuscule following by a majuscule, for example 'hMxxx', otherwise jackson
    // mapper will serialize as 'hmxxx' by default
    @JsonProperty("fNormTolAlg")
    private double fNormTolAlg;

    private double initialAddTolAlg;

    private double scStepTolAlg;

    private double mxNewTStepAlg;

    private int msbsetAlg;

    private int mxIterAlg;

    private int printFlAlg;

    @JsonProperty("fNormTolAlgJ")
    private double fNormTolAlgJ;

    private double initialAddTolAlgJ;

    private double scStepTolAlgJ;

    private double mxNewTStepAlgJ;

    private int msbsetAlgJ;

    private int mxIterAlgJ;

    private int printFlAlgJ;

    @JsonProperty("fNormTolAlgInit")
    private double fNormTolAlgInit;

    private double initialAddTolAlgInit;

    private double scStepTolAlgInit;

    private double mxNewTStepAlgInit;

    private int msbsetAlgInit;

    private int mxIterAlgInit;

    private int printFlAlgInit;

    private int maximumNumberSlowStepIncrease;

    private double minimalAcceptableStep;

    @Override
    public void writeParameter(XMLStreamWriter writer) throws XMLStreamException {
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, F_NORM_TOL_ALG, Double.toString(fNormTolAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INITIAL_ADD_TOL_ALG, Double.toString(initialAddTolAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, SC_STEP_TOL_ALG, Double.toString(scStepTolAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MX_NEW_T_STEP_ALG, Double.toString(mxNewTStepAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MSBSET_ALG, Integer.toString(msbsetAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MX_ITER_ALG, Integer.toString(mxIterAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, PRINT_FL_ALG, Integer.toString(printFlAlg));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, F_NORM_TOL_ALG_J, Double.toString(fNormTolAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INITIAL_ADD_TOL_ALG_J, Double.toString(initialAddTolAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, SC_STEP_TOL_ALG_J, Double.toString(scStepTolAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MX_NEWT_STEP_ALG_J, Double.toString(mxNewTStepAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MSBSET_ALG_J, Integer.toString(msbsetAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MX_ITER_ALG_J, Integer.toString(mxIterAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, PRINT_FL_ALG_J, Integer.toString(printFlAlgJ));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, F_NORM_TOL_ALG_INIT, Double.toString(fNormTolAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INITIAL_ADD_TOL_ALG_INIT, Double.toString(initialAddTolAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, SC_STEP_TOL_ALG_INIT, Double.toString(scStepTolAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MX_NEW_T_STEP_ALG_INIT, Double.toString(mxNewTStepAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MSBSET_ALG_INIT, Integer.toString(msbsetAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MX_ITER_ALG_INIT, Integer.toString(mxIterAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, PRINT_FL_ALG_INIT, Integer.toString(printFlAlgInit));
        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, MAXIMUM_NUMBER_SLOW_STEP_INCREASE, Integer.toString(maximumNumberSlowStepIncrease));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MINIMAL_ACCEPTABLE_STEP, Double.toString(minimalAcceptableStep));
    }
}
