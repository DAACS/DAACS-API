package com.daacs.framework.validation

import com.daacs.framework.validation.annotations.ValidBaseAssessment
import com.daacs.framework.validation.child.base.AssessmentCategoryValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.framework.validation.child.base.DomainsValidator
import com.daacs.framework.validation.child.base.ItemGroupValidator
import com.daacs.framework.validation.child.base.OverallRubricValidator
import com.daacs.framework.validation.child.base.ScoringTypeValidator
import com.daacs.model.assessment.Assessment

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
/**
 * Created by chostetter on 8/19/16.
 */

class BaseAssessmentValidator extends AbstractValidator implements ConstraintValidator<ValidBaseAssessment, Assessment> {

    private List<ChildValidator> childValidators = [
            new ScoringTypeValidator(),
            new AssessmentCategoryValidator(),
            new OverallRubricValidator(),
            new DomainsValidator(),
            new ItemGroupValidator()
    ];

    @Override
    public void initialize(ValidBaseAssessment constraintAnnotation) {}

    @Override
    public boolean isValid(Assessment assessment, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        return childValidators.findAll{ !it.isValid(assessment, context) }.size() == 0;
    }
}