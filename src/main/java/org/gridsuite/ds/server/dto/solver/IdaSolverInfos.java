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

    private int order;

    private double initStep;

    private double minStep;

    private double maxStep;

    private double absAccuracy;

    private double relAccuracy;

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

        writer.writeEndElement();
    }
}
