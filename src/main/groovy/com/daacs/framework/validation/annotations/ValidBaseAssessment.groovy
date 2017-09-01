package com.daacs.framework.validation.annotations

import com.daacs.framework.validation.BaseAssessmentValidator

import javax.validation.Constraint
import javax.validation.Payload
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Created by chostetter on 8/19/16.
 */

@Target([ElementType.TYPE, ElementType.ANNOTATION_TYPE])
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = [BaseAssessmentValidator.class])
public @interface ValidBaseAssessment {
    String message() default "";
    Class<?>[] groups() default [];
    Class<? extends Payload>[] payload() default [];
}

