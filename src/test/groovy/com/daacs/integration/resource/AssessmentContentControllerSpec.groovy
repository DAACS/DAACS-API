package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentContent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*

/**
 * Created by chostetter on 7/18/16.
 */

class AssessmentContentControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/assessment-contents"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper()

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

    def "get assessment content"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        AssessmentContent assessmentContent = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<AssessmentContent>(){})
        assessmentContent.assessmentId == "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
        assessmentContent.content.containsKey("landing")
    }

    def "get assessment content by category"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?assessmentCategory=MATHEMATICS"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        AssessmentContent assessmentContent = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<AssessmentContent>(){})
        assessmentContent.assessmentId == "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
        assessmentContent.content.containsKey("landing")
    }
}
