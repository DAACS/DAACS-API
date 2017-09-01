package com.daacs.framework.validation

import javax.validation.ConstraintValidatorContext

/**
 * Created by chostetter on 8/19/16.
 */
abstract class AbstractValidator {
    protected static void addPropertyViolation(ConstraintValidatorContext context, String propertyName, String message){
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyName)
                .addConstraintViolation();
    }
}
