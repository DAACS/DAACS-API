package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorContainer
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.*
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
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
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

    FileItemStream fileItemStream
    FileItemIterator fileItemIterator

    String getBasePath() {
        return "/assessments"
    }

    def setup() {
        objectMapper = new ObjectMapperConfig().objectMapper()
        writingUpdateAssessmentRequest = new WritingUpdateAssessmentRequest(scoringType: ScoringType.MANUAL,
                id: "writing-assessment-2",
                assessmentType: AssessmentType.WRITING_PROMPT,
                domains: [
                        new ScoringDomainRequest(
                                id: "domain-1",
                                label: "domain-1",
                                content: "",
                                rubric: new RubricRequest(
                                        completionScoreMap: [:],
                                        supplementTable: [
                                                new SupplementTableRowRequest(completionScore: CompletionScore.HIGH, content: "")
                                        ]))
                ],
                prerequisites: [],
                overallRubric: new RubricRequest(
                        completionScoreMap: [:],
                        supplementTable: [
                                new SupplementTableRowRequest(completionScore: CompletionScore.HIGH, content: "")
                        ]),
                assessmentCategory: AssessmentCategory.WRITING,
                enabled: false,
                label: "Writing",
                content: ["": ""],
                writingPrompt: new WritingPromptRequest(content: "")
        )

        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        assessmentService = new AssessmentServiceImpl()

        fileItemStream = Mock(FileItemStream)
        fileItemIterator = Mock(FileItemIterator)
        fileItemIterator.next() >> fileItemStream
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "get assessments"() {
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
        List<Assessment> assessments = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<Assessment>>() {
        })
        assessments.size() > 0
    }

    def "create new assessment"() {
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
        String id = jsonNode.get("data").get("id").toString()
        id != null
    }

    def "create new assessment w/nested domains"() {
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
        String id = jsonNode.get("data").get("id").toString()
        id != null
    }

    def "create new assessment (validation error)"() {
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
        List<ErrorContainer> errors = objectMapper.readValue(jsonNode.get("errors").toString(), new TypeReference<List<ErrorContainer>>() {
        });
        errors.size() == 1
        errors.get(0).code == "Assessment.constraintViolation"
        errors.get(0).meta.get("code") == "NotNull"
        errors.get(0).meta.get("field") == "scoringType"
    }

    def "update assessment"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        writingUpdateAssessmentRequest.scoringType = null
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
        !assessment.isValid

  //      List<ErrorContainer> meta = objectMapper.readValue(jsonNode.get("data").get("errors").toString(), ErrorContainer[].class)
//        meta.size() == 1
//        meta.get(0).code == 'Assessment.constraintViolation'
    //    meta.get(0).detail == 'scoringType may not be null'

        when:
        //put it back
        writingUpdateAssessmentRequest.enabled = true
        writingUpdateAssessmentRequest.scoringType = ScoringType.MANUAL
        updateAssessmentRequest = new DataWrapper(data: writingUpdateAssessmentRequest);
        request = new HttpEntity<DataWrapper<UpdateAssessmentRequest>>(updateAssessmentRequest, headers);
        response = restTemplate.exchange(
                serviceURI("/" + writingUpdateAssessmentRequest.id),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode fixedJsonNode = objectMapper.readTree(response.getBody())
        Assessment fixedAssessment = objectMapper.readValue(fixedJsonNode.get("data").toString(), Assessment)
        fixedAssessment.id == writingUpdateAssessmentRequest.id
        fixedAssessment.enabled
        fixedAssessment.isValid

       // List<ErrorContainer> fixedMeta = objectMapper.readValue(fixedJsonNode.get("data").get("errors").toString(), ErrorContainer[].class)
        // fixedMeta.size() == 0
    }

    private static String getAssessmentJson(String path) {
        DefaultResourceLoader loader = new DefaultResourceLoader();
        String json = new String(Files.readAllBytes(Paths.get(loader.getResource(path).getURI())));
        return json;
    }

    def "uploadLightSideModel: success"() {
        setup:
        headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Bearer " +  oauthToken)

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>()
        parts.add("file", new FileSystemResource(new ClassPathResource("sample.xml").getFile()))
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(parts, headers)

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/upload-lightside-models"),
                HttpMethod.POST,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
    }

    def "get assessment"() {
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
        Assessment assessment = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<CATAssessment>() {
        })
        assessment.id == "32c91abf-fc9b-4b41-ac4e-3f36b3e323d8"
    }
}
