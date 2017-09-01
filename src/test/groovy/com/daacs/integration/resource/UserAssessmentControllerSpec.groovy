package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorContainer
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.user.*
import com.daacs.model.dto.CreateUserAssessmentRequest
import com.daacs.model.dto.UpdateUserAssessmentRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class UserAssessmentControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/user-assessments"
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

    def "create new user assessment"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<CreateUserAssessmentRequest> createUserAssessmentRequest =
                new DataWrapper(data: new CreateUserAssessmentRequest(assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"));

        HttpEntity<DataWrapper<CreateUserAssessmentRequest>> request = new HttpEntity<DataWrapper<CreateUserAssessmentRequest>>(createUserAssessmentRequest, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        UserAssessment userAssessment = objectMapper.readValue(jsonNode.get("data").toString(), CATUserAssessment)
        userAssessment.assessmentId == "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
    }

    def "update user assessment (auto grade)"(){
        setup:
        String userAssessmentId = "42c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<UpdateUserAssessmentRequest> updateUserAssessmentRequest =
                new DataWrapper(data: new UpdateUserAssessmentRequest(id: userAssessmentId, status: CompletionStatus.COMPLETED));

        HttpEntity<DataWrapper<UpdateUserAssessmentRequest>> request = new HttpEntity<DataWrapper<UpdateUserAssessmentRequest>>(updateUserAssessmentRequest, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/" + userAssessmentId),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        UserAssessment userAssessment = objectMapper.readValue(jsonNode.get("data").toString(), CATUserAssessment)
        userAssessment.id == userAssessmentId
        userAssessment.status == CompletionStatus.GRADED
    }

    def "update user assessment (validation error)"(){
        setup:
        String userAssessmentId = "42c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<UpdateUserAssessmentRequest> updateUserAssessmentRequest =
                new DataWrapper(data: new UpdateUserAssessmentRequest(id: userAssessmentId));

        HttpEntity<DataWrapper<UpdateUserAssessmentRequest>> request = new HttpEntity<DataWrapper<UpdateUserAssessmentRequest>>(updateUserAssessmentRequest, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/" + userAssessmentId),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<ErrorContainer> errors = objectMapper.readValue(jsonNode.get("errors").toString(), new TypeReference<List<ErrorContainer>>(){});
        errors.size() == 1
        errors.get(0).code == "updateUserAssessmentRequest.constraintViolation"
        errors.get(0).meta.get("code") == "NotNull"
        errors.get(0).meta.get("field") == "status"
    }

    def "update user assessment (manual grade)"(){
        setup:
        oauthToken = null
        login("testadmin123")

        String userAssessmentId = "42c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
        headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + oauthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        DataWrapper<UpdateUserAssessmentRequest> updateUserAssessmentRequest =
                new DataWrapper(data: new UpdateUserAssessmentRequest(
                        id: userAssessmentId,
                        status: CompletionStatus.GRADED,
                        userId: "9999",
                        domainScores: [
                                new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.MEDIUM),
                                new DomainScore(domainId: "domain-2", rubricScore: CompletionScore.MEDIUM)
                        ],
                        overallScore: CompletionScore.MEDIUM));

        HttpEntity<DataWrapper<UpdateUserAssessmentRequest>> request = new HttpEntity<DataWrapper<UpdateUserAssessmentRequest>>(updateUserAssessmentRequest, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/" + userAssessmentId),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        UserAssessment userAssessment = objectMapper.readValue(jsonNode.get("data").toString(), CATUserAssessment)
        userAssessment.id == userAssessmentId
        userAssessment.status == CompletionStatus.GRADED
    }

    def "get user assessment"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/42c91abf-fc9b-4b41-ac4e-3f36b3e323d8"),
                HttpMethod.GET,
                httpEntity,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        UserAssessment userAssessment = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<CATUserAssessment>(){})
        userAssessment.id == "42c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
    }
}
