package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.user.CompletionSummary
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.UserAssessmentSummary
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class SystemControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/system"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper()
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "get completed assessments"() {
        setup:
        headers.add("Authorization", "Basic " + getBasicAuthHeaderValue())
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/completed-user-assessments?&startDate=2016-07-01T00:00:00.000Z&endDate=2016-07-30T00:00:00.000Z"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserAssessmentSummary> userAssessmentSummaries = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserAssessmentSummary>>(){})
        userAssessmentSummaries.size() > 0
    }

    def "get completion summary"() {
        setup:
        headers.add("Authorization", "Basic " + getBasicAuthHeaderValue())
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/completion-summary?username=testuser123"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        CompletionSummary completionSummary = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<CompletionSummary>(){})
        completionSummary.getHasCompletedAllCategories()
    }

    def "queue canvas submissions for all students"() {
        setup:
        headers.add("Authorization", "Basic " + getBasicAuthHeaderValue())
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/queue-canvas-submission-updates"),
                HttpMethod.PUT,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
    }

    def "calculate grades : graded"() {
        setup:
        headers.add("Authorization", "Basic " + getBasicAuthHeaderValue())
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);
        AssessmentCategory[] assessmentCategories = [AssessmentCategory.MATHEMATICS, AssessmentCategory.READING];
        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/calculate-grades?assessmentCategories[]=MATHEMATICS&assessmentCategories[]=READING&startDate=2016-07-01T00:00:00.000Z&endDate=2017-07-30T00:00:00.000Z&dryRun=false"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK

    }

    def "calculate grades : ungraded"() {
        setup:
        headers.add("Authorization", "Basic " + getBasicAuthHeaderValue())
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/calculate-grades?assessmentCategories[]=MATHEMATICS&assessmentCategories[]=READING&completionStatus=COMPLETED&startDate=2016-07-01T00:00:00.000Z&endDate=2017-07-30T00:00:00.000Z&dryRun=false"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK

    }

    def "calculate grades : invalid completion status"() {
        setup:
        headers.add("Authorization", "Basic " + getBasicAuthHeaderValue())
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/calculate-grades?assessmentCategories[]=MATHEMATICS&assessmentCategories[]=READING&completionStatus=IN_PROGRESS&startDate=2016-07-01T00:00:00.000Z&endDate=2017-07-30T00:00:00.000Z&dryRun=false"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST

    }

    def "calculate grades (with error)"() {
        setup:
        headers.add("Authorization", "Basic " + getBasicAuthHeaderValue())
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/calculate-grades?assessmentCategories[]=MATHEMATIC&startDate=2016-07-01T00:00:00.000Z&endDate=2017-07-30T00:00:00.000Z&dryRun=false"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR

    }

}
