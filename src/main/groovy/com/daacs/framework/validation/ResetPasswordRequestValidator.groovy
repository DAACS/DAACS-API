package com.daacs.framework.validation

import com.daacs.framework.validation.annotations.ValidResetPasswordRequest
import com.daacs.model.dto.ResetPasswordRequest

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
/**
 * Created by chostetter on 3/2/17.
 */

class ResetPasswordRequestValidator extends AbstractValidator implements ConstraintValidator<ValidResetPasswordRequest, ResetPasswordRequest> {

    @Override
    public void initialize(ValidResetPasswordRequest constraintAnnotation) {}

    @Override
    public boolean isValid(ResetPasswordRequest resetPasswordRequest, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();

        if (!resetPasswordRequest.password.equals(resetPasswordRequest.passwordConfirm)) {
            addPropertyViolation(context, "password", "must match confirm password field");
            return false;
        }

        return true;
    }
}