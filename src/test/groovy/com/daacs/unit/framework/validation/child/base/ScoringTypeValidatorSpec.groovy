package com.daacs.unit.framework.validation.child.base

import com.daacs.framework.validation.child.base.ScoringTypeValidator
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.AssessmentType
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.assessment.ScoringType
import com.daacs.unit.framework.validation.child.ValidatorSpec
import spock.lang.Unroll

import static com.daacs.model.assessment.AssessmentType.*
import static com.daacs.model.assessment.ScoringType.*
/**
 * Created by chostetter on 8/22/16.
 */
class ScoringTypeValidatorSpec extends ValidatorSpec {
    ScoringTypeValidator validator;

    def setup(){
        setupContext()

        validator = new ScoringTypeValidator()
    }

    def "test constraint violation build"(){
        setup:
        Assessment assessment = Mock(Assessment)
        assessment.getAssessmentType() >> WRITING_PROMPT
        assessment.getScoringType() >> AVERAGE

        when:
        validator.isValid(assessment, context)

        then:
        1 * context.buildConstraintViolationWithTemplate(_) >> constraintViolationBuilder
        1 * constraintViolationBuilder.addPropertyNode(_) >> nodeBuilderCustomizableContext
        1 * nodeBuilderCustomizableContext.addConstraintViolation()
    }

    def "validScoringTypes does not have entry"(){
        setup:
        Assessment assessment = new CATAssessment(assessmentType: CAT, scoringType: AVERAGE)
        validator.validScoringTypes = [:]

        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        !isValid
    }

    @Unroll
    def "test validation"(AssessmentType assessmentType, ScoringType scoringType, boolean expectedIsValid){
        setup:
        Assessment assessment = Mock(Assessment)
        assessment.getAssessmentType() >> assessmentType
        assessment.getScoringType() >> scoringType

        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        isValid == expectedIsValid

        where:
        assessmentType  | scoringType | expectedIsValid
        CAT             | AVERAGE     | true
        CAT             | MANUAL      | false
        CAT             | SUM         | false
        CAT             | LIGHTSIDE   | false
        LIKERT          | AVERAGE     | true
        LIKERT          | MANUAL      | false
        LIKERT          | SUM         | true
        LIKERT          | LIGHTSIDE   | false
        MULTIPLE_CHOICE | AVERAGE     | true
        MULTIPLE_CHOICE | MANUAL      | false
        MULTIPLE_CHOICE | SUM         | true
        MULTIPLE_CHOICE | LIGHTSIDE   | false
        WRITING_PROMPT  | AVERAGE     | false
        WRITING_PROMPT  | MANUAL      | true
        WRITING_PROMPT  | SUM         | false
        WRITING_PROMPT  | LIGHTSIDE   | true
    }
}