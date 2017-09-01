package com.daacs.framework.validation

import com.daacs.framework.validation.annotations.ValidWritingAssessment
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.framework.validation.child.writing.LightSideConfigValidator
import com.daacs.model.assessment.WritingAssessment

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

/**
 * Created by chostetter on 9/1/16.
 */

class WritingAssessmentValidator extends AbstractValidator implements ConstraintValidator<ValidWritingAssessment, WritingAssessment> {

    private List<ChildValidator> childValidators = [
            new LightSideConfigValidator()
    ];

    @Override
    public void initialize(ValidWritingAssessment constraintAnnotation) {}

    @Override
    public boolean isValid(WritingAssessment assessment, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        return childValidators.findAll{ !it.isValid(assessment, context) }.size() == 0;
    }

}