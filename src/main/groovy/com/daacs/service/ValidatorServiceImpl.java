package com.daacs.service;

import com.daacs.framework.exception.ConstraintViolationException;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.validation.SmartValidator;

/**
 * Created by chostetter on 9/1/16.
 */
@Service
public class ValidatorServiceImpl implements ValidatorService {
    private static final Logger log = LoggerFactory.getLogger(ValidatorServiceImpl.class);

    @Autowired
    private SmartValidator validator;

    @Override
    public Try<Void> validate(Object object, Class<?> valueType, Object... validationHints){
        BindingResult bindingResult = new DirectFieldBindingResult(object, valueType.getName());

        validator.validate(object, bindingResult, validationHints);

        if(bindingResult.getAllErrors().size() > 0){
            bindingResult.getAllErrors().forEach(it -> log.error(it.toString()));
            return new Try.Failure<>(new ConstraintViolationException(bindingResult.getAllErrors()));
        }

        return new Try.Success<>(null);
    }
}
