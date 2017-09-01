package com.daacs.unit.service

import com.daacs.framework.validation.annotations.group.CreateGroup
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.CATAssessment
import com.daacs.service.ValidatorService
import com.daacs.service.ValidatorServiceImpl
import com.lambdista.util.Try
import org.springframework.validation.BindingResult
import org.springframework.validation.ObjectError
import org.springframework.validation.SmartValidator
import spock.lang.Specification
/**
 * Created by chostetter on 9/1/16.
 */
class ValidationServiceSpec extends Specification {

    ValidatorService validatorService;
    SmartValidator validator;

    def setup(){
        validator = Mock(SmartValidator)
        validatorService = new ValidatorServiceImpl(validator: validator)
    }

    def "validate: success"(){
        setup:
        Assessment assessment = new CATAssessment()

        when:
        Try<Void> maybeValidated = validatorService.validate(assessment, Assessment.class, CreateGroup.class)

        then:
        1 * validator.validate(assessment, _, CreateGroup.class)

        then:
        maybeValidated.isSuccess()
    }

    def "validate: failed validation"(){
        setup:
        Assessment assessment = new CATAssessment()

        when:
        Try<Void> maybeValidated = validatorService.validate(assessment, Assessment.class, CreateGroup.class)

        then:
        1 * validator.validate(assessment, _, CreateGroup.class) >> { args ->
            BindingResult bindingResult = args[1]
            bindingResult.addError(new ObjectError("assessment", "validation failed"))
        }

        then:
        maybeValidated.isFailure()
    }
}
