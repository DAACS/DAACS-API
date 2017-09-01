package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.event.ErrorEvent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.http.*

/**
 * Created by chostetter on 7/18/16.
 */

class ErrorEventControllerSpec extends RestControllerSpec {


    String getBasePath() {
        return "/error-events"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper()

        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "create new error event"(){
        setup:
        ErrorEvent requestBody = new ErrorEvent()

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
        ErrorEvent errorEvent = objectMapper.readValue(jsonNode.get("data").toString(), ErrorEvent)
        errorEvent.ipAddress != null
    }
}
