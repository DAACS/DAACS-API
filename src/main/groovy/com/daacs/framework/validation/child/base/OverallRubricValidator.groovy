package com.daacs.framework.validation.child.base

import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.ItemGroupAssessment
import com.daacs.model.assessment.WritingAssessment

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat

/**
 * Created by chostetter on 8/19/16.
 */
public class OverallRubricValidator extends RubricValidator implements ChildValidator<Assessment> {

    @Override
    public boolean isValid(Assessment assessment, ConstraintValidatorContext context) {

        switch (assessment.getAssessmentType()) {
            case AssessmentType.WRITING_PROMPT:

                if (assessment instanceof WritingAssessment) {
                    return isValid((WritingAssessment) assessment, context, null);
                } else {
                    return buildAndReturnAssessmentTypePropertyViolation(context, assessment.getAssessmentType())
                }

            case AssessmentType.LIKERT:
            case AssessmentType.MULTIPLE_CHOICE:
            case AssessmentType.CAT:
                if (assessment instanceof ItemGroupAssessment) {
                    return isValid((ItemGroupAssessment) assessment, context, null);
                } else {
                    return buildAndReturnAssessmentTypePropertyViolation(context, assessment.getAssessmentType())
                }

            default:
                return buildAndReturnAssessmentTypePropertyViolation(context, assessment.getAssessmentType())
        }
    }
}
