package com.daacs.unit.framework.validation.child

import spock.lang.Specification

import javax.validation.ConstraintValidatorContext

/**
 * Created by chostetter on 8/22/16.
 */
abstract class ValidatorSpec extends Specification {
    ConstraintValidatorContext context;
    ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilder;
    ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderCustomizableContext;

    def setupContext(){
        context = Mock(ConstraintValidatorContext)

        constraintViolationBuilder = Mock(ConstraintValidatorContext.ConstraintViolationBuilder)
        nodeBuilderCustomizableContext = Mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext)

        context.buildConstraintViolationWithTemplate(_) >> constraintViolationBuilder
        constraintViolationBuilder.addPropertyNode(_) >> nodeBuilderCustomizableContext
    }
}
