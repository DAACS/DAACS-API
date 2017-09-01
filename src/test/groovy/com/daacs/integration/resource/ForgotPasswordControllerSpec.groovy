package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.dto.ForgotPasswordRequest
import com.daacs.model.dto.ResetPasswordRequest
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class ForgotPasswordControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/forgot-password"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper();

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "forgot password request"(){
        setup:
        ForgotPasswordRequest requestBody = new ForgotPasswordRequest(
                username: "testuser123@test.com"
        );

        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", objectMapper.readTree(objectMapper.writeValueAsString(requestBody)));
        HttpEntity<ObjectNode> request = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

    def "reset password"(){
        setup:
        ResetPasswordRequest requestBody = new ResetPasswordRequest(
                userId: "9999",
                password: "testpassword123",
                passwordConfirm: "testpassword123",
                code: "123456"
        );

        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", objectMapper.readTree(objectMapper.writeValueAsString(requestBody)));
        HttpEntity<ObjectNode> request = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

}
