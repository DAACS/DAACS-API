package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.WritingPromptUserAssessment
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*

import java.text.MessageFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
/**
 * Created by chostetter on 7/18/16.
 */

class DownloadControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/download"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper();
        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("/token")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "get exported user assessments"() {
        setup:
        List<ZipEntry> zipEntries = [];
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        //get downloadToken
        ResponseEntity<String> tokenResponse = restTemplate.exchange(
                serviceURI("/token"),
                HttpMethod.GET,
                httpEntity,
                String.class);

        then:
        tokenResponse.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(tokenResponse.getBody())
        String downloadToken = jsonNode.get("data").get("downloadToken").asText()


        when:
        ResponseEntity<byte[]> response = restTemplate.exchange(
                serviceURI(MessageFormat.format("/manual-grade-user-assessments?downloadToken={0}", downloadToken)),
                HttpMethod.GET,
                new HttpEntity<Object>(new HashMap<>()),
                byte[].class);

        then:
        response.statusCode == HttpStatus.OK
        response.getHeaders().get("Content-Disposition").get(0).contains("filename=")
        response.getHeaders().get("Content-Disposition").get(0).contains(".zip")
        ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(response.getBody()));

        then:
        ZipEntry entry;
        while((entry = zipInputStream.getNextEntry()) != null){
            zipEntries.add(entry);
        }

        then:
        zipEntries.size() > 0
        zipEntries.each{
            assert it.getName().endsWith(".json")
        }
    }

    def "get exported user assessment"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        //get downloadToken
        ResponseEntity<String> tokenResponse = restTemplate.exchange(
                serviceURI("/token"),
                HttpMethod.GET,
                httpEntity,
                String.class);

        then:
        tokenResponse.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(tokenResponse.getBody())
        String downloadToken = jsonNode.get("data").get("downloadToken").asText()

        when:
        ResponseEntity<byte[]> response = restTemplate.exchange(
                serviceURI(MessageFormat.format("/user-assessments?id=user-assessment-1&userId=user123&downloadToken={0}", downloadToken)),
                HttpMethod.GET,
                new HttpEntity<Object>(new HashMap<>()),
                byte[].class);

        then:
        response.statusCode == HttpStatus.OK
        response.getHeaders().get("Content-Disposition").get(0).contains("filename=")
        response.getHeaders().get("Content-Disposition").get(0).contains(".json")

        UserAssessment userAssessment = objectMapper.readValue(response.getBody(), WritingPromptUserAssessment.class)
        userAssessment.getId() == "user-assessment-1"
    }


    def "get exported assessment"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        //get downloadToken
        ResponseEntity<String> tokenResponse = restTemplate.exchange(
                serviceURI("/token"),
                HttpMethod.GET,
                httpEntity,
                String.class);

        then:
        tokenResponse.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(tokenResponse.getBody())
        String downloadToken = jsonNode.get("data").get("downloadToken").asText()

        when:
        ResponseEntity<byte[]> response = restTemplate.exchange(
                serviceURI(MessageFormat.format("/assessments?id=writing-assessment-1&downloadToken={0}", downloadToken)),
                HttpMethod.GET,
                new HttpEntity<Object>(new HashMap<>()),
                byte[].class);

        then:
        response.statusCode == HttpStatus.OK
        response.getHeaders().get("Content-Disposition").get(0).contains("filename=")
        response.getHeaders().get("Content-Disposition").get(0).contains(".json")
        response.getHeaders().get("Content-Type").get(0).contains("application/json")

        Assessment assessment = objectMapper.readValue(response.getBody(), Assessment.class)
        assessment.assessmentType == AssessmentType.WRITING_PROMPT
    }
}
