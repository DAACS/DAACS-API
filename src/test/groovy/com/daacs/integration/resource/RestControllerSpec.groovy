package com.daacs.integration.resource

import com.daacs.Application
import com.daacs.integration.resource.model.AuthResponseDTO
import com.daacs.integration.resource.model.DataWrapper
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.embedded.LocalServerPort
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import spock.lang.Specification
/**
 * Created by chostetter on 7/18/16.
 */

@ContextConfiguration(classes = [Application.class])
@ActiveProfiles(["test"])
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
abstract class RestControllerSpec extends Specification {
    @LocalServerPort
    int port

    HttpHeaders headers
    String oauthToken
    TestRestTemplate restTemplate = new TestRestTemplate();

    ObjectMapper objectMapper

    abstract String getBasePath()

    URI serviceURI(String path = "") {
        new URI("http://localhost:$port${basePath}${path}")
    }

    def login(String username = "testuser123", String password = "testpassword123"){
        if(oauthToken != null) return;

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
        map.add("grant_type", "password");
        map.add("username", username);
        map.add("password", password);
        map.add("scope", "read write");
        map.add("client_id", "web");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
        ResponseEntity<DataWrapper<AuthResponseDTO>> response = restTemplate.exchange(
                new URI("http://localhost:$port/oauth/token"),
                HttpMethod.POST,
                request,
                DataWrapper.class);

        assert response.getStatusCode() == HttpStatus.OK

        oauthToken = ((DataWrapper<AuthResponseDTO>)response.getBody()).getData().access_token
    }

    String getBasicAuthHeaderValue(String username = "testsystemuser123", String password = "testpassword123"){
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
