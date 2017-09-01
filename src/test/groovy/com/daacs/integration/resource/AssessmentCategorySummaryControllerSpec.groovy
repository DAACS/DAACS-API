package com.daacs.integration.resource

import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategorySummary
import com.daacs.model.assessment.AssessmentSummary
import org.springframework.http.*

/**
 * Created by chostetter on 4/4/17.
 */

class AssessmentCategorySummaryControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/assessment-category-summaries"
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

    def "get assessment category summaries for current user"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<AssessmentSummary>>> response = restTemplate.exchange(serviceURI(""), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<AssessmentCategorySummary> assessmentCategorySummaries = ((List<AssessmentCategorySummary>)response.getBody().getData());
        assessmentCategorySummaries.size() > 0
    }

    def "get assessment category summaries for different user"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<AssessmentSummary>>> response = restTemplate.exchange(serviceURI("?userId=8888"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<AssessmentCategorySummary> assessmentCategorySummaries = ((List<AssessmentCategorySummary>)response.getBody().getData());
        assessmentCategorySummaries.size() > 0
    }
}
