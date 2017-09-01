package com.daacs.framework.validation.child.cat

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.CATAssessment

import javax.validation.ConstraintValidatorContext
/**
 * Created by chostetter on 8/19/16.
 */
class MinMaxTakenGroupValidator extends AbstractValidator implements ChildValidator<CATAssessment> {

    @Override
    boolean isValid(CATAssessment assessment, ConstraintValidatorContext context) {
        if(assessment.getMinTakenGroups() <= 0){
            addPropertyViolation(context, "minTakenGroups", "must be larger than 0");
            return false;
        }

        if(assessment.getMinTakenGroups() > assessment.getMaxTakenGroups()){
            addPropertyViolation(context, "maxTakenGroups", "must be equal to or larger than minTakenGroups");
            return false;
        }

        return true;
    }
}
