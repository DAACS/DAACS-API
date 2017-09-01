package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.UserAssessmentSummary
import com.daacs.model.dto.ScoringClass
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class UserAssessmentSummaryControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/user-assessment-summaries"
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

    def "get user summaries"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?assessmentId=32c91abf-fc9b-4b41-ac4e-3f36b3e323d8&takenDate=2016-01-01T00:00:00.000Z"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() == 1
        userAssessmentSummaries.get(0).assessmentId == "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
    }

    def "get user summaries by status (COMPLETED)"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?status=COMPLETED"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() == 4
        userAssessmentSummaries.each{
            assert it.status == CompletionStatus.COMPLETED
        }
    }

    def "get user summaries by status and scoring (COMPLETED, AUTO)"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?status=COMPLETED&scoring=AUTO"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() > 0
        userAssessmentSummaries.each{
            assert it.status == CompletionStatus.COMPLETED
        }
        userAssessmentSummaries.each{
            assert ScoringClass.AUTO.getScoringTypes().contains(it.scoringType)
        }
    }

    def "get user summaries by status and scoring (COMPLETED, MANUAL)"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?status=COMPLETED&scoring=MANUAL"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() > 0
        userAssessmentSummaries.each{
            assert it.status == CompletionStatus.COMPLETED
        }
        userAssessmentSummaries.each{
            assert ScoringClass.MANUAL.getScoringTypes().contains(it.scoringType)
        }
    }

    def "get user summaries by status (COMPLETED & IN_PROGRESS)"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?status=COMPLETED&status=IN_PROGRESS"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() == 10
        userAssessmentSummaries.findAll { it.status == CompletionStatus.COMPLETED }.size() == 4
        userAssessmentSummaries.findAll { it.status == CompletionStatus.IN_PROGRESS }.size() == 6
    }

    def "get latest summary"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?assessmentId=32c91abf-fc9b-4b41-ac4e-3f36b3e323d8&limit=1"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() == 1
        userAssessmentSummaries.get(0).assessmentId == "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
    }

    def "get latest summary by category"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("?assessmentCategory=MATHEMATICS&limit=1"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() == 1
        userAssessmentSummaries.get(0).assessmentCategory == AssessmentCategory.MATHEMATICS
    }

}
