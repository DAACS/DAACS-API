package com.daacs.framework.validation

import com.daacs.framework.validation.annotations.ValidCATAssessment
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.framework.validation.child.cat.CATItemGroupValidator
import com.daacs.framework.validation.child.cat.ItemGroupTransitionValidator
import com.daacs.framework.validation.child.cat.MinMaxTakenGroupValidator
import com.daacs.framework.validation.child.cat.NumQuestionsPerGroupValidator
import com.daacs.model.assessment.CATAssessment

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

/**
 * Created by chostetter on 8/19/16.
 */

class CATAssessmentValidator extends AbstractValidator implements ConstraintValidator<ValidCATAssessment, CATAssessment> {

    private List<ChildValidator> childValidators = [
            new MinMaxTakenGroupValidator(),
            new NumQuestionsPerGroupValidator(),
            new ItemGroupTransitionValidator(),
            new CATItemGroupValidator()
    ];

    @Override
    public void initialize(ValidCATAssessment constraintAnnotation) {}

    @Override
    public boolean isValid(CATAssessment assessment, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        return childValidators.findAll{ !it.isValid(assessment, context) }.size() == 0;
    }

}