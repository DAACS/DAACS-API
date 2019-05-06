package com.daacs.framework.validation.child.base

import com.daacs.framework.validation.AbstractValidator
import com.daacs.framework.validation.child.ChildValidator
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.ScoringType

import javax.validation.ConstraintValidatorContext
import java.text.MessageFormat

/**
 * Created by chostetter on 8/19/16.
 */
class ScoringTypeValidator extends AbstractValidator implements ChildValidator<Assessment> {

    private Map<AssessmentType, List<ScoringType>> validScoringTypes = [
            (AssessmentType.CAT): [ScoringType.AVERAGE],
            (AssessmentType.LIKERT): [ScoringType.SUM, ScoringType.AVERAGE],
            (AssessmentType.MULTIPLE_CHOICE): [ScoringType.SUM, ScoringType.AVERAGE],
            (AssessmentType.WRITING_PROMPT): [ScoringType.LIGHTSIDE, ScoringType.MANUAL]
    ];

    @Override
    boolean isValid(Assessment assessment, ConstraintValidatorContext context) {
        if(!validScoringTypes.containsKey(assessment.getAssessmentType())){
            addPropertyViolation(context,
                    "scoringType",
                    MessageFormat.format(
                            "Could not find valid scoringTypes for assessment w/type {0}",
                            assessment.getAssessmentType()));

            return false;
        }

        if(!validScoringTypes.get(assessment.getAssessmentType()).contains(assessment.getScoringType())){
            addPropertyViolation(context,
                    "scoringType",
                    MessageFormat.format(
                            "Invalid scoringType for assessment w/type {0}, expecting {1}",
                            assessment.getAssessmentType(),
                            validScoringTypes.get(assessment.getAssessmentType())));

            return false;
        }

        return true;
    }
}
