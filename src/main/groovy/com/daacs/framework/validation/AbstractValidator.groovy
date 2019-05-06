package com.daacs.framework.validation

import com.daacs.model.assessment.AssessmentType

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat

/**
 * Created by chostetter on 8/19/16.
 */
abstract class AbstractValidator {
    protected static void addPropertyViolation(ConstraintValidatorContext context, String propertyName, String message){
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyName)
                .addConstraintViolation();
    }

    protected boolean buildAndReturnAssessmentTypePropertyViolation(ConstraintValidatorContext context, AssessmentType assessmentType) {
        addPropertyViolation(context, "assessmentType.invalid", MessageFormat.format("Invalid assessmentType: {0}", assessmentType))
        return false;
    }
}
