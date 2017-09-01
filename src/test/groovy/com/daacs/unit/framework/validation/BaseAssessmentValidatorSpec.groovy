package com.daacs.unit.framework.validation

import com.daacs.framework.validation.BaseAssessmentValidator
import com.daacs.framework.validation.child.base.AssessmentCategoryValidator
import com.daacs.model.assessment.Assessment
import com.daacs.unit.framework.validation.child.ValidatorSpec
/**
 * Created by chostetter on 8/23/16.
 */
class BaseAssessmentValidatorSpec extends ValidatorSpec {

    BaseAssessmentValidator validator;
    AssessmentCategoryValidator assessmentCategoryValidator;
    Assessment assessment;

    def setup(){
        setupContext()
        assessmentCategoryValidator = Mock(AssessmentCategoryValidator)
        assessment = Mock(Assessment)

        validator = new BaseAssessmentValidator(childValidators: [ assessmentCategoryValidator ])
    }

    def "isValid calls childValidators"(){
        when:
        boolean isValid = validator.isValid(assessment, context)

        then:
        1 * assessmentCategoryValidator.isValid(assessment, context) >> true

        then:
        isValid
    }
}
