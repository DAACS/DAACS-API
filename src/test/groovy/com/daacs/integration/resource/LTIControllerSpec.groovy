package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.WritingPromptUserAssessment
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.http.*
import org.springframework.test.context.TestPropertySource

import javax.servlet.http.HttpServletRequest
import java.text.MessageFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
/**
 * Created by mgoldman on 4/10/19.
 */

class LTIControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/lti"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper();

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "lti launch success"() {
        setup:
        HttpServletRequest requestBody = Mock(HttpServletRequest);

        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.set("data", objectMapper.readTree(objectMapper.writeValueAsString(requestBody)));
        HttpEntity<ObjectNode> httpEntity = new HttpEntity<ObjectNode>(dataNode, headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(serviceURI(""), HttpMethod.POST, httpEntity, String.class)

        then:
        response.statusCode == HttpStatus.FOUND
    }

}
