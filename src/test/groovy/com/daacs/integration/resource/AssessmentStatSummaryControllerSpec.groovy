package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentStatSummary
import com.daacs.service.AssessmentService
import com.daacs.service.AssessmentServiceImpl
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*

/**
 * Created by adistasio on 10/13/16.
 */

class AssessmentStatSummaryControllerSpec extends RestControllerSpec {

    AssessmentService assessmentService

    String getBasePath() {
        return "/assessment-stat-summaries"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper()

        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        assessmentService = new AssessmentServiceImpl()
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "getAssessmentStats"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<AssessmentStatSummary> assessmentStats = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<AssessmentStatSummary>>(){})
        assessmentStats.size() > 0
    }
}
