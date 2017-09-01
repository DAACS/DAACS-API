package com.daacs.unit.framework.validation

import com.daacs.framework.validation.ResetPasswordRequestValidator
import com.daacs.model.dto.ResetPasswordRequest
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 4/10/17.
 */
class ResetPasswordRequestValidatorSpec extends ValidatorSpec {

    ResetPasswordRequestValidator validator
    ResetPasswordRequest request

    def setup(){
        setupContext()
        validator = new ResetPasswordRequestValidator()
    }

    def "isValid"(){
        setup:
        request = new ResetPasswordRequest(password: "abc123", passwordConfirm: "abc123")

        when:
        boolean isValid = validator.isValid(request, context)

        then:
        isValid
    }

    def "!isValid"(){
        setup:
        request = new ResetPasswordRequest(password: "abc123", passwordConfirm: "dafsa")

        when:
        boolean isValid = validator.isValid(request, context)

        then:
        !isValid
    }
}
