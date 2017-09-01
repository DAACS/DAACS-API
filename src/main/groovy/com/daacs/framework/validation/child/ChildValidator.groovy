package com.daacs.framework.validation.child

import javax.validation.ConstraintValidatorContext

/**
 * Created by chostetter on 8/19/16.
 */
 interface ChildValidator<T> {
     public boolean isValid(T parent, ConstraintValidatorContext context);
}
