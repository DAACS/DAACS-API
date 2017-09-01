package com.daacs.framework.validation.annotations

import com.daacs.framework.validation.CreateUserRequestValidator

import javax.validation.Constraint
import javax.validation.Payload
import java.lang.annotation.*
/**
 * Created by chostetter on 8/19/16.
 */

@Target([ElementType.TYPE, ElementType.ANNOTATION_TYPE])
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = [CreateUserRequestValidator.class])
@Documented
public @interface ValidCreateUserRequest {
    String message() default "";
    Class<?>[] groups() default [];
    Class<? extends Payload>[] payload() default [];
}

