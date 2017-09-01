package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.model.ErrorResponse
import com.daacs.model.event.EventType
import com.daacs.model.event.UserEvent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class UserEventControllerSpec extends RestControllerSpec {


    String getBasePath() {
        return "/user-events"
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

    def "create new user event"(){
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        UserEvent requestBody = new UserEvent(eventType: EventType.LOGIN)

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
        UserEvent userEvent = objectMapper.readValue(jsonNode.get("data").toString(), UserEvent)
        userEvent.getEventType() == EventType.LOGIN
    }
}
