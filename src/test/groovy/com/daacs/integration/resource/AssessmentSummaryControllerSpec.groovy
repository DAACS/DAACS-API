package com.daacs.integration.resource

import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentSummary
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class AssessmentSummaryControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/assessment-summaries"
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

    def "get enabled assessment summaries only"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<AssessmentSummary>>> response = restTemplate.exchange(serviceURI("?enabled=true"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<AssessmentSummary> assessmentSummaries = ((List<AssessmentSummary>)response.getBody().getData());
        assessmentSummaries.size() > 0
        assessmentSummaries.findAll{ it.enabled }.size() == assessmentSummaries.size()
    }

    def "get enabled assessment summaries for different user"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<AssessmentSummary>>> response = restTemplate.exchange(serviceURI("?enabled=true&userId=8888"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<AssessmentSummary> assessmentSummaries = ((List<AssessmentSummary>)response.getBody().getData());
        assessmentSummaries.size() > 0
        assessmentSummaries.findAll{ it.enabled }.size() == assessmentSummaries.size()
        assessmentSummaries.findAll{ it.userAssessmentSummary != null }.size() == 1
    }

    def "get disabled assessment summaries only"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<AssessmentSummary>>> response = restTemplate.exchange(serviceURI("?enabled=false"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<AssessmentSummary> assessmentSummaries = ((List<AssessmentSummary>)response.getBody().getData());
        assessmentSummaries.size() == 0
    }
}
