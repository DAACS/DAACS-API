package com.daacs.framework.validation.annotations

import com.daacs.framework.validation.ResetPasswordRequestValidator

import javax.validation.Constraint
import javax.validation.Payload
import java.lang.annotation.*

/**
 * Created by chostetter on 3/2/17.
 */

@Target([ElementType.TYPE, ElementType.ANNOTATION_TYPE])
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = [ResetPasswordRequestValidator.class])
@Documented
public @interface ValidResetPasswordRequest {
    String message() default "";
    Class<?>[] groups() default [];
    Class<? extends Payload>[] payload() default [];
}

