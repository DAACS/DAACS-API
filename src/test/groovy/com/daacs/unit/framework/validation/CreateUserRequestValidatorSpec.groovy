package com.daacs.unit.framework.validation

import com.daacs.framework.validation.CreateUserRequestValidator
import com.daacs.model.dto.CreateUserRequest
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/23/16.
 */
class CreateUserRequestValidatorSpec extends ValidatorSpec {

    CreateUserRequestValidator validator;
    CreateUserRequest createUserRequest;

    def setup(){
        setupContext()
        createUserRequest = new CreateUserRequest(
                username: "test",
                firstName: "Bob",
                lastName: "Barker",
                password: "pass",
                passwordConfirm: "pass",
                role: "ROLE_STUDENT"
        )
        validator = new CreateUserRequestValidator()
    }

    def "isValid ROLE_STUDENT"(){
        when:
        boolean isValid = validator.isValid(createUserRequest, context)

        then:
        isValid
    }

    def "isValid ROLE_INSTRUCTOR"(){
        when:
        createUserRequest.setRole("ROLE_INSTRUCTOR")
        boolean isValid = validator.isValid(createUserRequest, context)

        then:
        isValid
    }

    def "isValid: false passwords don't match"(){
        setup:
        createUserRequest.passwordConfirm = "test"

        when:
        boolean isValid = validator.isValid(createUserRequest, context)

        then:
        !isValid
    }

    def "isValid: false when role is invalid"(){
        setup:
        createUserRequest.role = "ROLE_ADMIN"

        when:
        boolean isValid = validator.isValid(createUserRequest, context)

        then:
        !isValid
    }
}
