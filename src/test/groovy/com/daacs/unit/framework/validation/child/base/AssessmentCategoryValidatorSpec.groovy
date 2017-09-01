package com.daacs.unit.framework.validation.child.base

import com.daacs.framework.validation.child.base.AssessmentCategoryValidator
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentCategory
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.CATAssessment
import com.daacs.unit.framework.validation.child.ValidatorSpec
import spock.lang.Unroll

import static com.daacs.model.assessment.AssessmentCategory.*
import static com.daacs.model.assessment.AssessmentType.*
/**
 * Created by chostetter on 8/22/16.
 */
class AssessmentCategoryValidatorSpec extends ValidatorSpec {
    AssessmentCategoryValidator validator;

    def setup(){
        setupContext()

        validator = new AssessmentCategoryValidator()
    }

    def "test constraint violation build"(){
        setup:
        Assessment assessment = Mock(Assessment)
        assessment.getAssessmentType() >> WRITING_PROMPT
        assessment.getAssessmentCategory() >> MATHEMATICS

        when:
        validator.isValid(assessment, context)

        then:
        1 * context.buildConstraintViolationWithTemplate(_) >> constraintViolationBuilder
        1 * constraintViolationBuilder.addPropertyNode(_) >> nodeBuilderCustomizableContext
        1 * nodeBuilderCustomizableContext.addConstraintViolation()
    }

    def "validScoringTypes does not have entry"(){
        setup:
        Assessment assessment = new CATAssessment(assessmentType: CAT, assessmentCategory: MATHEMATICS)
        validator.validCategories = [:]

        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        !isValid
    }

    @Unroll
    def "test validation"(AssessmentType assessmentType, AssessmentCategory assessmentCategory, boolean expectedIsValid){
        setup:
        Assessment assessment = Mock(Assessment)
        assessment.getAssessmentType() >> assessmentType
        assessment.getAssessmentCategory() >> assessmentCategory

        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        isValid == expectedIsValid

        where:
        assessmentType  | assessmentCategory | expectedIsValid
        CAT             | MATHEMATICS        | true
        CAT             | READING            | true
        CAT             | COLLEGE_SKILLS     | false
        CAT             | WRITING            | false
        LIKERT          | MATHEMATICS        | false
        LIKERT          | READING            | false
        LIKERT          | COLLEGE_SKILLS     | true
        LIKERT          | WRITING            | false
        MULTIPLE_CHOICE | MATHEMATICS        | true
        MULTIPLE_CHOICE | READING            | true
        MULTIPLE_CHOICE | COLLEGE_SKILLS     | true
        MULTIPLE_CHOICE | WRITING            | false
        WRITING_PROMPT  | MATHEMATICS        | false
        WRITING_PROMPT  | READING            | false
        WRITING_PROMPT  | COLLEGE_SKILLS     | false
        WRITING_PROMPT  | WRITING            | true
    }
}