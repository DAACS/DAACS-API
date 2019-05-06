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
import com.daacs.service.AssessmentCategoryGroupService
import com.daacs.service.AssessmentServiceImpl
import com.daacs.service.hystrix.AssessmentCategoryGroupServiceImpl
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
 * Created by mgoldman on 3/1/19.
 */

class AssessmentCategoryGroupControllerSpec extends RestControllerSpec {

    AssessmentCategoryGroupService assessmentCategoryGroupService

    FileItemStream fileItemStream
    FileItemIterator fileItemIterator

    AssessmentCategoryGroup dummyGroup


    String getBasePath() {
        return "/assessment-category-groups"
    }

    def setup() {
        objectMapper = new ObjectMapperConfig().objectMapper()

        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        assessmentCategoryGroupService = new AssessmentCategoryGroupServiceImpl()

        fileItemStream = Mock(FileItemStream)
        fileItemIterator = Mock(FileItemIterator)
        fileItemIterator.next() >> fileItemStream

        dummyGroup = new AssessmentCategoryGroup(
                id: "id",
                label: "lebleele",
                assessmentCategory: "MATHEMATICS"
        )
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "get assessmentsCategoryGroups"() {
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
        List<AssessmentCategoryGroup> groups = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<AssessmentCategoryGroup>>() {
        })
        groups.size() > 0
    }

    def "create new AssessmentCategoryGroup"() {
        setup:
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + oauthToken)
        AssessmentCategoryGroup requestBody = dummyGroup

        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", objectMapper.readTree(objectMapper.writeValueAsString(requestBody)));
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
        AssessmentCategoryGroup group = objectMapper.readValue(jsonNode.get("data").toString(), AssessmentCategoryGroup)
        group.id == "id"
    }

    def "update new AssessmentCategoryGroup"() {
        setup:
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + oauthToken)
        AssessmentCategoryGroup requestBody = dummyGroup

        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", objectMapper.readTree(objectMapper.writeValueAsString(requestBody)));
        HttpEntity<ObjectNode> request = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/id"),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        AssessmentCategoryGroup group = objectMapper.readValue(jsonNode.get("data").toString(), AssessmentCategoryGroup)
        group.id == "id"
    }

    def "delete new AssessmentCategoryGroup"() {
        setup:
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + oauthToken)

        ObjectNode dataNode = objectMapper.createObjectNode();
        HttpEntity<ObjectNode> request = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/id"),
                HttpMethod.DELETE,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.NO_CONTENT
    }
}
