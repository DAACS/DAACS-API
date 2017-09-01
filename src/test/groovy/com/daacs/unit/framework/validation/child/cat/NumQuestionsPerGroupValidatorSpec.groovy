package com.daacs.unit.framework.validation.child.cat

import com.daacs.framework.validation.child.cat.NumQuestionsPerGroupValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.Item
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/22/16.
 */
class NumQuestionsPerGroupValidatorSpec extends ValidatorSpec {
    NumQuestionsPerGroupValidator validator;
    CATAssessment catAssessment;

    def setup(){
        setupContext()
        validator = new NumQuestionsPerGroupValidator()

        catAssessment = new CATAssessment(
                numQuestionsPerGroup: 2,
                itemGroups: [
                        new CATItemGroup(items: [new Item(), new Item()])
                ])
    }

    def "isValid: passes"(){
        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        isValid
    }

    def "isValid: fails"(){
        setup:
        catAssessment = new CATAssessment(
                numQuestionsPerGroup: 2,
                itemGroups: [
                        new CATItemGroup(items: [new Item()])
                ])

        when:
        boolean isValid = validator.isValid(catAssessment, context)

        then:
        !isValid
    }
}