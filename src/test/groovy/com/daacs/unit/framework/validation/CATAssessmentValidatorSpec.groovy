package com.daacs.unit.framework.validation

import com.daacs.framework.validation.CATAssessmentValidator
import com.daacs.framework.validation.child.cat.NumQuestionsPerGroupValidator
import com.daacs.model.assessment.CATAssessment
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/23/16.
 */
class CATAssessmentValidatorSpec extends ValidatorSpec {

    CATAssessmentValidator validator;
    NumQuestionsPerGroupValidator numQuestionsPerGroupValidator;
    CATAssessment assessment;

    def setup(){
        setupContext()
        numQuestionsPerGroupValidator = Mock(NumQuestionsPerGroupValidator)
        assessment = Mock(CATAssessment)

        validator = new CATAssessmentValidator(childValidators: [ numQuestionsPerGroupValidator ])
    }

    def "isValid calls childValidators"(){
        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        1 * numQuestionsPerGroupValidator.isValid(assessment, context) >> true

        then:
        isValid
    }
}
