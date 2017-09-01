package com.daacs.integration.resource

import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorResponse
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.ItemGroup
import com.daacs.model.item.WritingPrompt
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class AnswerControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return ""
    }

    def setup(){
        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "attempt w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("/user-assessment-question-answers")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "get question answers"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<ItemGroup>>> response = restTemplate.exchange(
                serviceURI("/user-assessment-question-answers?assessmentId=32c91abf-fc9b-4b41-ac4e-3f36b3e323d8&takenDate=2016-01-01T00:00:00.000Z&domainId=domain-1"),
                HttpMethod.GET,
                httpEntity,
                DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<ItemGroup> itemGroups = ((List<ItemGroup>)response.getBody().getData());
        itemGroups.size() == 1
        ((CATItemGroup) itemGroups.get(0)).getItems().get(0).question == "abc?"
    }

    def "get writing sample"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<List<WritingPrompt>>> response = restTemplate.exchange(
                serviceURI("/user-assessment-writing-sample-answers?assessmentId=32c91abf-fc9b-4b41-ac4e-3f36b3e323d6&takenDate=2014-01-01T00:00:00.000Z"),
                HttpMethod.GET,
                httpEntity,
                DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        List<WritingPrompt> writingSamples = ((List<WritingPrompt>)response.getBody().getData());
        writingSamples.size() == 1
        writingSamples.get(0).sample == "this is my writing sample"
    }
}
