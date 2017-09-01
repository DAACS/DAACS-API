package com.daacs.framework.validation

import com.daacs.framework.validation.annotations.ValidCreateUserRequest
import com.daacs.model.dto.CreateUserRequest

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
/**
 * Created by chostetter on 8/19/16.
 */

class CreateUserRequestValidator extends AbstractValidator implements ConstraintValidator<ValidCreateUserRequest, CreateUserRequest> {

    @Override
    public void initialize(ValidCreateUserRequest constraintAnnotation) {}

    @Override
    public boolean isValid(CreateUserRequest createUserRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (!createUserRequest.password.equals(createUserRequest.passwordConfirm)) {
            addPropertyViolation(context, "password", "must match confirm password field");
            return false;
        }

        if (!createUserRequest.role.equals("ROLE_STUDENT")) {
            addPropertyViolation(context, "role", "must be ROLE_STUDENT");
            return false;
        }

        return true;
    }
}