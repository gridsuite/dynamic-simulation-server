package org.gridsuite.ds.server.service.dynamicmapping.implementation;

import com.powsybl.commons.PowsyblException;
import org.gridsuite.ds.server.dto.dynamicmapping.Script;
import org.gridsuite.ds.server.service.dynamicmapping.DynamicMappingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;

@Service
public class DynamicMappingServiceImpl implements DynamicMappingService {
    private final String baseUri;
    private RestTemplate restTemplate;

    public DynamicMappingServiceImpl(@Value("${dynamic-mapping-server.base-uri:http://dynamic-mapping-server/}") String baseUri) {
        this.baseUri = baseUri;
    }

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplateBuilder().build();
    }

    @Override
    public Script createFromMapping(String mappingName) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = baseUri + DELIMITER + DYNAMIC_MAPPING_SCRIPT_CREATE_END_POINT + DELIMITER + mappingName + "?persistent=false";
        var uriBuilder = UriComponentsBuilder.fromHttpUrl(url);

        // call time-series Rest API
        var responseEntity = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, new HttpEntity<>("", headers), Script.class);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            return responseEntity.getBody();
        } else {
            throw new PowsyblException("Can not send time series to server: HttpStatus = " + responseEntity.getStatusCode());
        }
    }
}
