package com.daacs.integration.resource

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.integration.resource.model.DataWrapper
import com.daacs.model.ErrorResponse
import com.daacs.model.SessionedUser
import com.daacs.model.UserSearchResult
import com.daacs.model.dto.CreateUserRequest
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.*
/**
 * Created by chostetter on 7/18/16.
 */

class UserControllerSpec extends RestControllerSpec {

    String getBasePath() {
        return "/users"
    }

    def setup(){
        objectMapper = new ObjectMapperConfig().objectMapper();

        login();
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
    }

    def "attempt get user w/out session auth header"() {
        setup:
        RequestEntity request = RequestEntity.get(serviceURI("/me")).build()

        when:
        ResponseEntity<ErrorResponse> response = restTemplate.exchange(request, ErrorResponse)

        then:
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    def "login, attempt get user with session auth header"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<SessionedUser>> response = restTemplate.exchange(serviceURI("/me"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.data.username == "testuser123"
        response.body.data.id == "9999"
    }

    def "get user by ID"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<SessionedUser>> response = restTemplate.exchange(serviceURI("/9999"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.data.username == "testuser123"
        response.body.data.id == "9999"
    }

    def "get user by username"() {
        setup:

        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<DataWrapper<SessionedUser>> response = restTemplate.exchange(serviceURI("?username=testuser123"), HttpMethod.GET, httpEntity, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.data.get(0).id == "9999"
    }

    def "search users"() {
        setup:
        headers.add("Authorization", "Bearer " + oauthToken)
        HttpEntity<?> httpEntity = new HttpEntity<Object>(headers);

        when:
        ResponseEntity<String> response = restTemplate.exchange(serviceURI("?keywords=dummy%20user"), HttpMethod.GET, httpEntity, String.class)

        then:
        response.statusCode == HttpStatus.OK
        JsonNode jsonNode = objectMapper.readTree(response.getBody())
        List<UserSearchResult> userSearchResults = objectMapper.readValue(jsonNode.get("data").toString(), new TypeReference<List<UserSearchResult>>(){})
        userSearchResults.size() == 1
        userSearchResults.get(0).firstName == "dummy"
        userSearchResults.get(0).lastName == "user"
        userSearchResults.get(0).username == "dummyuser"
    }

    def "create user"() {
        setup:
        DataWrapper<CreateUserRequest> createUserRequest =
                new DataWrapper(data: new CreateUserRequest(username: "test", firstName: "first", lastName: "last", password: "pass", passwordConfirm: "pass", role: "ROLE_STUDENT"));
        HttpEntity<DataWrapper<CreateUserRequest>> request = new HttpEntity<DataWrapper<CreateUserRequest>>(createUserRequest, headers);

        when:
        ResponseEntity<DataWrapper<SessionedUser>> response = restTemplate.exchange(serviceURI(""), HttpMethod.POST, request, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.OK
        response.body.data.username == "test"
    }

    def "create user with bad role fails"() {
        setup:
        DataWrapper<CreateUserRequest> createUserRequest =
                new DataWrapper(data: new CreateUserRequest(username: "test", firstName: "first", lastName: "last", password: "pass", passwordConfirm: "pass", role: "ROLE_ADMIN"));
        HttpEntity<DataWrapper<CreateUserRequest>> request = new HttpEntity<DataWrapper<CreateUserRequest>>(createUserRequest, headers);

        when:
        ResponseEntity<DataWrapper<SessionedUser>> response = restTemplate.exchange(serviceURI(""), HttpMethod.POST, request, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "create user with mismatch passwords fails"() {
        setup:
        DataWrapper<CreateUserRequest> createUserRequest =
                new DataWrapper(data: new CreateUserRequest(username: "test", firstName: "first", lastName: "last", password: "pass", passwordConfirm: "doh", role: "ROLE_STUDENT"));
        HttpEntity<DataWrapper<CreateUserRequest>> request = new HttpEntity<DataWrapper<CreateUserRequest>>(createUserRequest, headers);

        when:
        ResponseEntity<DataWrapper<SessionedUser>> response = restTemplate.exchange(serviceURI(""), HttpMethod.POST, request, DataWrapper.class)

        then:
        response.statusCode == HttpStatus.BAD_REQUEST
    }
}
