package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorContainer
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.model.dto.UpdateAssessmentRequest
import com.daacs.model.dto.WritingUpdateAssessmentRequest
import com.daacs.model.dto.assessmentUpdate.RubricRequest
import com.daacs.model.dto.assessmentUpdate.ScoringDomainRequest
import com.daacs.model.dto.assessmentUpdate.SupplementTableRowRequest
import com.daacs.model.dto.assessmentUpdate.WritingPromptRequest
import com.daacs.service.AssessmentService
import com.daacs.service.AssessmentServiceImpl
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by chostetter on 7/18/16.
 */

class AssessmentControllerSpec extends RestControllerSpec {

    AssessmentService assessmentService
    WritingUpdateAssessmentRequest writingUpdateAssessmentRequest

    String getBasePath() {
        return "/assessments"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper()
        writingUpdateAssessmentRequest = new WritingUpdateAssessmentRequest(
                id: "writing-assessment-2",
                enabled: false,
                assessmentType: AssessmentType.WRITING_PROMPT,
                content: [:],
                domains: [new ScoringDomainRequest(content: "Domain",
                                            id: "domain-1",
                                            rubric: new RubricRequest(supplementTable: [new SupplementTableRowRequest(content: "test", completionScore: CompletionScore.LOW, contentSummary: "summary")]))],
                overallRubric: new RubricRequest(supplementTable: [new SupplementTableRowRequest(content: "test", completionScore: CompletionScore.LOW, contentSummary: "summary")]),
                writingPrompt: new WritingPromptRequest(content: "Writing Prompt"),
        )


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

    def "get assessments"(){
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
        List<Assessment> assessments = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<Assessment>>(){})
        assessments.size() > 0
    }

    def "create new assessment"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        JsonNode assessmentJson = objectMapper.readTree(getAssessmentJson("classpath:/assessment_examples/mathematics.json"));
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", assessmentJson);

        HttpEntity<ObjectNode> request = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        Assessment assessment = objectMapper.readValue(jsonNode.get("data").toString(), Assessment)
        assessment != null
    }

    def "create new assessment w/nested domains"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        JsonNode assessmentJson = objectMapper.readTree(getAssessmentJson("classpath:/assessment_examples/mathematics_nested_domains.json"));
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", assessmentJson);

        HttpEntity<ObjectNode> request = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        Assessment assessment = objectMapper.readValue(jsonNode.get("data").toString(), Assessment)
        assessment != null
    }

    def "create new assessment (validation error)"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        JsonNode assessmentJson = objectMapper.readTree(getAssessmentJson("classpath:/assessment_examples/mathematics_validation_error.json"));
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", assessmentJson);

        HttpEntity<ObjectNode> request = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI(""),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<ErrorContainer> errors = objectMapper.readValue(jsonNode.get("errors").toString(), new TypeReference<List<ErrorContainer>>(){});
        errors.size() == 1
        errors.get(0).code == "assessment.constraintViolation"
        errors.get(0).meta.get("code") == "NotNull"
        errors.get(0).meta.get("field") == "scoringType"
    }

    def "update assessment"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<UpdateAssessmentRequest> updateAssessmentRequest =
                new DataWrapper(data: writingUpdateAssessmentRequest);

        HttpEntity<DataWrapper<UpdateAssessmentRequest>> request = new HttpEntity<DataWrapper<UpdateAssessmentRequest>>(updateAssessmentRequest, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/" + writingUpdateAssessmentRequest.id),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        Assessment assessment = objectMapper.readValue(jsonNode.get("data").toString(), Assessment)
        assessment.id == writingUpdateAssessmentRequest.id
        !assessment.enabled

        when:
        //put it back
        writingUpdateAssessmentRequest.enabled = true
        updateAssessmentRequest = new DataWrapper(data: writingUpdateAssessmentRequest);
        request = new HttpEntity<DataWrapper<UpdateAssessmentRequest>>(updateAssessmentRequest, headers);
        response = restTemplate.exchange(
                serviceURI("/" + writingUpdateAssessmentRequest.id),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
    }

    private static String getAssessmentJson(String path){
        DefaultResourceLoader loader = new DefaultResourceLoader();
        String json = new String(Files.readAllBytes(Paths.get(loader.getResource(path).getURI())));
        return json;
    }

    def "update writing assessment: success"(){
        setup:
        String assessmentId = "writing-assessment-1"
        headers.add("Authorization", "Bearer " + oauthToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("scoringType", "LIGHTSIDE");
        parts.add("lightside_domain-1", new FileSystemResource(new ClassPathResource("sample.xml").getFile()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(parts, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/" + assessmentId + "/upload-lightside-models"),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
    }

    def "update writing assessment: fails missing overall"(){
        setup:
        String assessmentId = "writing-assessment-1"
        headers.add("Authorization", "Bearer " + oauthToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("scoringType", "LIGHTSIDE");
        parts.add("lightside_overall", new FileSystemResource(new ClassPathResource("sample.xml").getFile()));
        parts.add("lightside_domain-1", new FileSystemResource(new ClassPathResource("sample.xml").getFile()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(parts, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/" + assessmentId + "/upload-lightside-models"),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "update writing assessment: fails missing domain"(){
        setup:
        String assessmentId = "writing-assessment-1"
        headers.add("Authorization", "Bearer " + oauthToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
        parts.add("scoringType", "LIGHTSIDE");
        parts.add("lightside_overall", new FileSystemResource(new ClassPathResource("sample.xml").getFile()));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(parts, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/" + assessmentId + "/upload-lightside-models"),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "get assessment"(){
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
        Assessment assessment = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<CATAssessment>(){})
        assessment.id == "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
    }
}
