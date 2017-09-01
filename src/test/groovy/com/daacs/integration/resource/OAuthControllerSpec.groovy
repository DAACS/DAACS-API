package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class OAuthControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/oauth"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper();

        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "remove token"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken);
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(serviceURI("/remove-token"), HttpMethod.GET, httpEntity, String.class)

        then:
        response.statusCode == HttpStatus.OK
    }

}
