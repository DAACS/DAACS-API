package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

/**
 * Created by chostetter on 4/6/17.
 */
public class NotEnabledException extends FailureTypeException {

    public NotEnabledException(String entityType, String detail) {
        super(
                getCode(entityType),
                detail,
                FailureType.NOT_RETRYABLE
        );
    }

    private static String getCode(String entityType){
        return entityType + ".notEnabled";
    }
}
