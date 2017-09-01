package com.daacs.integration.resource

import com.daacs.integration.resource.model.DataWrapper
import org.springframework.http.*

/**
 * Created by chostetter on 7/18/16.
 */

class SamlControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/saml"
    }

    def setup(){
        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }


    def "saml get registered login endpoints"() {
        setup:
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<Map<String, List<String>>>> response = restTemplate.exchange(serviceURI("/url"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        Map<String, List<String>> urlMap = ((DataWrapper<List<String>>) response.getBody()).getData()
        urlMap.containsKey("urls")
        urlMap.get("urls")
    }


}
