/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.solver;

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
public class IdaSolverInfos extends AbstractSolverInfos {

    public static final String SOLVER_ORDER = "order";
    public static final String INIT_STEP = "initStep";
    public static final String MIN_STEP = "minStep";
    public static final String MAX_STEP = "maxStep";
    public static final String ABS_ACCURACY = "absAccuracy";
    public static final String REL_ACCURACY = "relAccuracy";

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

    private int order;

    private double initStep;

    private double minStep;

    private double maxStep;

    private double absAccuracy;

    private double relAccuracy;

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

        XmlSerializableParameter.writeParameter(writer, ParameterType.INT, SOLVER_ORDER, Integer.toString(order));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, INIT_STEP, Double.toString(initStep));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MIN_STEP, Double.toString(minStep));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, MAX_STEP, Double.toString(maxStep));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, ABS_ACCURACY, Double.toString(absAccuracy));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, REL_ACCURACY, Double.toString(relAccuracy));
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
