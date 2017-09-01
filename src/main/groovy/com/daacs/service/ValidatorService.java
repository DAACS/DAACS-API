package com.daacs.service;

import com.lambdista.util.Try;

/**
 * Created by chostetter on 9/1/16.
 */
public interface ValidatorService {
    Try<Void> validate(Object object, Class<?> valueType, Object... validationHints);
}
