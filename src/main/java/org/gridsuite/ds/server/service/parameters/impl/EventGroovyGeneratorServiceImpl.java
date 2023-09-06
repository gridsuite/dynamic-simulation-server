/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.ds.server.service.parameters.impl;

import com.powsybl.commons.PowsyblException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.gridsuite.ds.server.dto.event.EventInfos;
import org.gridsuite.ds.server.dto.event.EventPropertyInfos;
import org.gridsuite.ds.server.service.parameters.EventGroovyGeneratorService;
import org.gridsuite.ds.server.utils.PropertyType;
import org.gridsuite.ds.server.utils.Utils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.stringtemplate.v4.ST;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Thang PHAM <quyet-thang.pham at rte-france.com>
 */
@Service
public class EventGroovyGeneratorServiceImpl implements EventGroovyGeneratorService {
    @Override
    public String generate(List<EventInfos> events) {
        Objects.requireNonNull(events);

        String eventsTemplate;
        String eventTemplate;
        String eventPropertyTemplate;
        try {
            eventsTemplate = IOUtils.toString(new ClassPathResource(EVENTS_TEMPLATE_DIR + RESOURCE_PATH_DELIMETER + "events.st").getInputStream(), Charset.defaultCharset());
            eventTemplate = IOUtils.toString(new ClassPathResource(EVENTS_TEMPLATE_DIR + RESOURCE_PATH_DELIMETER + "event.st").getInputStream(), Charset.defaultCharset());
            eventPropertyTemplate = IOUtils.toString(new ClassPathResource(EVENTS_TEMPLATE_DIR + RESOURCE_PATH_DELIMETER + "eventProperty.st").getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new PowsyblException("Unable to load templates for groovy script generation : " + e.getMessage());
        }
        // config root template
        ST eventsST = new ST(eventsTemplate);

        String[] eventStringList = events.stream().map(event -> generateEvent(eventTemplate, eventPropertyTemplate, event)).toArray(String[]::new);
        eventsST.add("events", eventStringList);

        return eventsST.render();
    }

    private String generateEvent(String eventTemplate, String eventPropertyTemplate, EventInfos event) {
        ST eventST = new ST(eventTemplate);
        eventST.add("eventType", event.getEventType());

        // --- add properties ---
        // static id property
        eventST.add("staticId", event.getEquipmentId());
        // other properties
        String[] propertyStringList = event.getProperties().stream()
                .map(property -> generateEventProperty(eventPropertyTemplate, property))
                .filter(property -> !StringUtils.isEmpty(property)).toArray(String[]::new);
        eventST.add("properties", propertyStringList);

        return eventST.render();
    }

    private String generateEventProperty(String eventPropertyTemplate, EventPropertyInfos property) {
        ST eventPropertyST = new ST(eventPropertyTemplate);
        String value = property.getValue();
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        // value =>  "value" when export groovy string value
        if (property.getType() == PropertyType.STRING) {
            value = Utils.convertStringToList(value).stream()
                    .map(elem -> "\"" + elem + "\"")
                    .collect(Collectors.joining(", "));
        }

        eventPropertyST.add("name", property.getName());
        eventPropertyST.add("value", value);

        return eventPropertyST.render();
    }
}
