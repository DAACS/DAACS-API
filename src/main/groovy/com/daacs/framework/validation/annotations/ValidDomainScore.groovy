package com.daacs.framework.validation.annotations

import com.daacs.framework.validation.DomainScoreValidator

import javax.validation.Constraint
import javax.validation.Payload
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
/**
 * Created by chostetter on 3/30/17.
 */

@Target([ElementType.TYPE, ElementType.ANNOTATION_TYPE])
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = [DomainScoreValidator.class])
public @interface ValidDomainScore {
    String message() default "";
    Class<?>[] groups() default [];
    Class<? extends Payload>[] payload() default [];
}