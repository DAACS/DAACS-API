package com.daacs.framework.validation.child.cat

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.item.CATItemGroup

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat

/**
 * Created by chostetter on 8/19/16.
 */
class NumQuestionsPerGroupValidator extends AbstractValidator implements ChildValidator<CATAssessment> {

    @Override
    boolean isValid(CATAssessment assessment, ConstraintValidatorContext context) {

        List<CATItemGroup> violatingGroups = assessment.getItemGroups().findAll{ itemGroup ->
            itemGroup.getItems().size() != assessment.getNumQuestionsPerGroup()
        }

        if(violatingGroups.size() > 0) {
            addPropertyViolation(context, "itemGroups", MessageFormat.format("All item groups must have (and only have) {0} questions", assessment.getNumQuestionsPerGroup()));
            return false;
        }

        return true;
    }
}
