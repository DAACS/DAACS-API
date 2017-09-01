package com.daacs.framework.validation.child.base

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.AssessmentType

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat

/**
 * Created by chostetter on 8/19/16.
 */
class AssessmentCategoryValidator extends AbstractValidator implements ChildValidator<Assessment> {

    private Map<AssessmentType, List<AssessmentCategory>> validCategories = [
            (AssessmentType.CAT): [AssessmentCategory.MATHEMATICS, AssessmentCategory.READING],
            (AssessmentType.LIKERT): [AssessmentCategory.COLLEGE_SKILLS],
            (AssessmentType.MULTIPLE_CHOICE): [AssessmentCategory.MATHEMATICS, AssessmentCategory.READING, AssessmentCategory.COLLEGE_SKILLS],
            (AssessmentType.WRITING_PROMPT): [AssessmentCategory.WRITING]
    ];

    @Override
    boolean isValid(Assessment assessment, ConstraintValidatorContext context) {
        if(!validCategories.containsKey(assessment.getAssessmentType())){
            addPropertyViolation(context,
                    "assessmentCategory",
                    MessageFormat.format(
                            "Could not find valid assessmentCategory for assessment w/type {0}",
                            assessment.getAssessmentType()));

            return false;
        }

        if(!validCategories.get(assessment.getAssessmentType()).contains(assessment.getAssessmentCategory())){
            addPropertyViolation(context,
                    "assessmentCategory",
                    MessageFormat.format(
                            "Invalid assessmentCategory for assessment w/type {0}, expecting {1}",
                            assessment.getAssessmentType(),
                            validCategories.get(assessment.getAssessmentType())
                    ));

            return false;
        }

        return true;
    }
}
