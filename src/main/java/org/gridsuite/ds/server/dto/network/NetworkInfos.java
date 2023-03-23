/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.dto.network;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.gridsuite.ds.server.dto.XmlSerializableParameter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@NoArgsConstructor
@Getter
@Setter
public class NetworkInfos implements XmlSerializableParameter {

    public static final String CAPACITOR_NO_RECLOSING_DELAY = "capacitor_no_reclosing_delay";
    public static final String DANGLING_LINE_CURRENT_LIMIT_MAX_TIME_OPERATION = "dangling_line_currentLimit_maxTimeOperation";
    public static final String LINE_CURRENT_LIMIT_MAX_TIME_OPERATION = "line_currentLimit_maxTimeOperation";
    public static final String LOAD_TP = "load_Tp";
    public static final String LOAD_TQ = "load_Tq";
    public static final String LOAD_ALPHA = "load_alpha";
    public static final String LOAD_ALPHA_LONG = "load_alphaLong";
    public static final String LOAD_BETA = "load_beta";
    public static final String LOAD_BETA_LONG = "load_betaLong";
    public static final String LOAD_IS_CONTROLLABLE = "load_isControllable";
    public static final String LOAD_IS_RESTORATIVE = "load_isRestorative";
    public static final String LOAD_Z_PMAX = "load_zPMax";
    public static final String LOAD_Z_QMAX = "load_zQMax";
    public static final String REACTANCE_NO_RECLOSING_DELAY = "reactance_no_reclosing_delay";
    public static final String TRANSFORMER_CURRENT_LIMIT_MAX_TIME_OPERATION = "transformer_currentLimit_maxTimeOperation";
    public static final String TRANSFORMER_T1ST_HT = "transformer_t1st_HT";
    public static final String TRANSFORMER_T1ST_THT = "transformer_t1st_THT";
    public static final String TRANSFORMER_TNEXT_HT = "transformer_tNext_HT";
    public static final String TRANSFORMER_TNEXT_THT = "transformer_tNext_THT";
    public static final String TRANSFORMER_TO_LV = "transformer_tolV";

    public static final String NETWORK_ID = "NETWORK";

    private String id = NETWORK_ID;

    private double capacitorNoReclosingDelay;

    private double danglingLineCurrentLimitMaxTimeOperation;

    private double lineCurrentLimitMaxTimeOperation;

    private double loadTp;

    private double loadTq;

    private double loadAlpha;

    private double loadAlphaLong;

    private double loadBeta;

    private double loadBetaLong;

    private boolean loadIsControllable;

    private boolean loadIsRestorative;

    private double loadZPMax;

    private double loadZQMax;

    private double reactanceNoReclosingDelay;

    private double transformerCurrentLimitMaxTimeOperation;

    private double transformerT1StHT;

    private double transformerT1StTHT;

    private double transformerTNextHT;

    private double transformerTNextTHT;

    private double transformerTolV;

    @Override
    public void writeParameter(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(DYN_BASE_URI, "set");
        writer.writeAttribute("id", id);

        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, CAPACITOR_NO_RECLOSING_DELAY, Double.toString(capacitorNoReclosingDelay));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, DANGLING_LINE_CURRENT_LIMIT_MAX_TIME_OPERATION, Double.toString(danglingLineCurrentLimitMaxTimeOperation));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LINE_CURRENT_LIMIT_MAX_TIME_OPERATION, Double.toString(lineCurrentLimitMaxTimeOperation));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_TP, Double.toString(loadTp));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_TQ, Double.toString(loadTq));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_ALPHA, Double.toString(loadAlpha));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_ALPHA_LONG, Double.toString(loadAlphaLong));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_BETA, Double.toString(loadBeta));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_BETA_LONG, Double.toString(loadBetaLong));
        XmlSerializableParameter.writeParameter(writer, ParameterType.BOOL, LOAD_IS_CONTROLLABLE, Boolean.toString(loadIsControllable));
        XmlSerializableParameter.writeParameter(writer, ParameterType.BOOL, LOAD_IS_RESTORATIVE, Boolean.toString(loadIsRestorative));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_Z_PMAX, Double.toString(loadZPMax));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, LOAD_Z_QMAX, Double.toString(loadZQMax));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, REACTANCE_NO_RECLOSING_DELAY, Double.toString(reactanceNoReclosingDelay));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, TRANSFORMER_CURRENT_LIMIT_MAX_TIME_OPERATION, Double.toString(transformerCurrentLimitMaxTimeOperation));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, TRANSFORMER_T1ST_HT, Double.toString(transformerT1StHT));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, TRANSFORMER_T1ST_THT, Double.toString(transformerT1StTHT));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, TRANSFORMER_TNEXT_HT, Double.toString(transformerTNextHT));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, TRANSFORMER_TNEXT_THT, Double.toString(transformerTNextTHT));
        XmlSerializableParameter.writeParameter(writer, ParameterType.DOUBLE, TRANSFORMER_TO_LV, Double.toString(transformerTolV));

        writer.writeEndElement();
    }
}
