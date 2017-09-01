package com.daacs.unit.framework.validation.child.cat

import com.daacs.framework.validation.child.cat.ItemGroupTransitionValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.item.Difficulty
import com.daacs.model.item.ItemGroupTransition
import com.daacs.unit.framework.validation.child.ValidatorSpec
import com.google.common.collect.Range
/**
 * Created by chostetter on 8/22/16.
 */
class ItemGroupTransitionValidatorSpec extends ValidatorSpec {
    ItemGroupTransitionValidator validator;
    CATAssessment catAssessment;

    def setup(){
        setupContext()
        validator = new ItemGroupTransitionValidator()

        catAssessment = new CATAssessment(
                itemGroupTransitions: [
                        new ItemGroupTransition(transitionMap: [
                                (Difficulty.EASY)  : Range.atMost(1),
                                (Difficulty.MEDIUM): Range.closed(2, 3),
                                (Difficulty.HARD)  : Range.atLeast(4)
                        ])
                ]
        )
    }

    def "isValid: passes"(){
        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        isValid
    }

    def "isValid: fails with gap"(){
        setup:
        catAssessment = new CATAssessment(
                itemGroupTransitions: [
                        new ItemGroupTransition(transitionMap: [
                                (Difficulty.EASY)  : Range.atMost(2),
                                (Difficulty.MEDIUM): Range.closed(3, 5)
                        ])
                ]
        )

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }

    def "isValid: fails with no transitions"(){
        setup:
        catAssessment = new CATAssessment(
                itemGroupTransitions: []
        )

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }
}