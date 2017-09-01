package com.daacs.integration.resource

import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorResponse
import com.daacs.model.dto.SendHelpEmailRequest
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class HelpControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/help"
    }

    def setup(){
        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "send help email"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<SendHelpEmailRequest> sendHelpEmailRequest =
                new DataWrapper(data: new SendHelpEmailRequest(assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8", text: "help text!", userAgent: "james bond v24"));

        HttpEntity<DataWrapper<SendHelpEmailRequest>> request = new HttpEntity<DataWrapper<SendHelpEmailRequest>>(sendHelpEmailRequest, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }

}
