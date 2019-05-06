package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorResponse
import com.daacs.model.dto.SaveItemGroupRequest
import com.daacs.model.dto.SaveWritingSampleRequest
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import com.daacs.model.item.ItemGroup
import com.daacs.model.item.WritingPrompt
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*

import java.time.Instant

/**
 * Created by chostetter on 7/18/16.
 */

class QuestionControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return ""
    }

    def setup() {
        objectMapper = new ObjectMapperConfig().objectMapper()

        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("/user-assessment-question-groups")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "get next item group"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<ItemGroup>>> response = restTemplate.exchange(
                serviceURI("/user-assessment-question-groups?assessmentId=32c91abf-fc9b-4b41-ac4e-3f36b3e323d5&limit=1"),
                HttpMethod.GET,
                httpEntity,
                DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<ItemGroup> itemGroups = ((List<ItemGroup>) response.getBody().getData());
        itemGroups.size() == 1
    }


    def "update item group"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<SaveItemGroupRequest> saveItemGroupRequest =
                new DataWrapper(data:
                        new SaveItemGroupRequest(
                                assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d9",
                                id: "itemgroup-1",
                                possibleItemAnswers: [
                                        new ItemAnswer(content: "one"),
                                        new ItemAnswer(content: "two")
                                ],
                                items: [
                                        new Item(
                                                id: "item-1",
                                                domainId: "domain-1",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-1", content: "", score: 0)],
                                                chosenItemAnswerId: "answer-1",
                                                question: "hey?",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()),
                                        new Item(
                                                id: "item-2",
                                                domainId: "domain-1",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-2", content: "", score: 0)],
                                                chosenItemAnswerId: "answer-2",
                                                question: "hey?",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now())
                                ]
                        ));

        HttpEntity<String> request = new HttpEntity<String>(objectMapper.writeValueAsString(saveItemGroupRequest), headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/user-assessment-question-groups/itemgroup-1"),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        ItemGroup itemGroup = objectMapper.readValue(jsonNode.get("data").toString(), ItemGroup)
        itemGroup.id == "itemgroup-1"
        itemGroup.items.size() == 2
    }

    def "update item group missing possibleItemAnswers.score passes"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<SaveItemGroupRequest> saveItemGroupRequest =
                new DataWrapper(data:
                        new SaveItemGroupRequest(
                                assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d9",
                                id: "itemgroup-1",
                                possibleItemAnswers: [
                                        new ItemAnswer(content: "one"),
                                        new ItemAnswer(content: "two")
                                ],
                                items: [
                                        new Item(
                                                id: "item-1",
                                                domainId: "domain-1",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-1", content: "", score: 0)],
                                                chosenItemAnswerId: "answer-1",
                                                question: "hey?",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now()),
                                        new Item(
                                                id: "item-2",
                                                domainId: "domain-1",
                                                possibleItemAnswers: [new ItemAnswer(id: "answer-2", content: "")],
                                                chosenItemAnswerId: "answer-2",
                                                question: "hey?",
                                                startDate: Instant.now(),
                                                completeDate: Instant.now())
                                ]
                        ));

        HttpEntity<String> request = new HttpEntity<String>(objectMapper.writeValueAsString(saveItemGroupRequest), headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/user-assessment-question-groups/itemgroup-1"),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
    }

    def "get writing sample"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<WritingPrompt>>> response = restTemplate.exchange(
                serviceURI("/user-assessment-writing-samples?assessmentId=32c91abf-fc9b-4b41-ac4e-3f36b3e323d6&limit=1"),
                HttpMethod.GET,
                httpEntity,
                DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<WritingPrompt> writingPrompts = ((List<WritingPrompt>) response.getBody().getData());
        writingPrompts.size() == 1
    }

    def "save writing sample"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)

        DataWrapper<SaveWritingSampleRequest> saveWritingSampleRequest =
                new DataWrapper(data:
                        new SaveWritingSampleRequest(
                                assessmentId: "32c91abf-fc9b-4b41-ac4e-3f36b3e323d6",
                                id: "writingsample-1",
                                sample: "writing sample!",
                                startDate: Instant.now(),
                                completeDate: Instant.now()
                        ));

        HttpEntity<String> request = new HttpEntity<String>(objectMapper.writeValueAsString(saveWritingSampleRequest), headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(
                serviceURI("/user-assessment-writing-samples/writingsample-1"),
                HttpMethod.PUT,
                request,
                String.class);

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        WritingPrompt writingSample = objectMapper.readValue(jsonNode.get("data").toString(), WritingPrompt)
        writingSample.id == "writingsample-1"
        writingSample.sample == "writing sample!"
    }


}
