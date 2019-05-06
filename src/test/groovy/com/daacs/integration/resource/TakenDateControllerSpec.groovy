package com.daacs.integration.resource

import com.daacs.component.utils.DefaultCatgoryGroup
import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.user.UserAssessmentTakenDate
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class TakenDateControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/user-assessment-taken-dates"
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

    def "get taken dates"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?assessmentCategoryGroupId="+ DefaultCatgoryGroup.MATHEMATICS_ID),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentTakenDate> takenDates = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentTakenDate>>(){});
        takenDates.size() == 4
    }

}
