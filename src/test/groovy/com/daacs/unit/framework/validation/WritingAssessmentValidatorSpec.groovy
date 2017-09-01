package com.daacs.unit.framework.validation

import com.daacs.framework.validation.WritingAssessmentValidator
import com.daacs.framework.validation.child.writing.LightSideConfigValidator
import com.daacs.model.assessment.WritingAssessment
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 9/1/16.
 */
class WritingAssessmentValidatorSpec extends ValidatorSpec {

    WritingAssessmentValidator validator;
    LightSideConfigValidator lightSideConfigValidator;
    WritingAssessment assessment;

    def setup(){
        setupContext()
        lightSideConfigValidator = Mock(LightSideConfigValidator)
        assessment = Mock(WritingAssessment)

        validator = new WritingAssessmentValidator(childValidators: [ lightSideConfigValidator ])
    }

    def "isValid calls childValidators"(){
        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        1 * lightSideConfigValidator.isValid(assessment, context) >> true

        then:
        isValid
    }
}
