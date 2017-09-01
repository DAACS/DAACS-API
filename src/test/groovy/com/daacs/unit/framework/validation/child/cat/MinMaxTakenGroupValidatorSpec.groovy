package com.daacs.unit.framework.validation.child.cat

import com.daacs.framework.validation.child.cat.MinMaxTakenGroupValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/22/16.
 */
class MinMaxTakenGroupValidatorSpec extends ValidatorSpec {
    MinMaxTakenGroupValidator validator;
    CATAssessment catAssessment;

    def setup(){
        setupContext()
        validator = new MinMaxTakenGroupValidator()

        catAssessment = new CATAssessment(
                minTakenGroups: 2,
                maxTakenGroups: 3)
    }

    def "isValid: passes"(){
        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        isValid
    }

    def "isValid: fails when minTakenGroups is larger than maxTakenGroups"(){
        setup:
        catAssessment.minTakenGroups = 5
        catAssessment.maxTakenGroups = 4

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }

    def "isValid: fails when minTakenGroups is lte 0"(){
        setup:
        catAssessment.minTakenGroups = 0

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }
}