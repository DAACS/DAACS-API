package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.WritingPromptUserAssessment
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.commons.httpclient.params.HttpParams
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.http.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.multipart.MultipartFile

import java.text.MessageFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Created by mgoldman on 11/06/18
 */

class UploadControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return ""
    }

    def setup() {
        objectMapper = new ObjectMapperConfig().objectMapper();
        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON)
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("/token")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "upload a file successfully"() {
        setup:
        headers = new HttpHeaders()
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Authorization", "Bearer " +  oauthToken)

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>()
        parts.add("image", new FileSystemResource(new ClassPathResource("small.jpeg").getFile()))
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(parts, headers)


        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/files"),
                HttpMethod.POST,
                request,
                String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        JsonNode urlNode = jsonNode.get("data")
        urlNode.toString() == "\"https://bucket.s3.amazonaws.com/bucketName/200.file\""
    }

}