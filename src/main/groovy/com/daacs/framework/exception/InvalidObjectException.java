package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

/**
 * Created by chostetter on 7/25/16.
 */
public class InvalidObjectException extends FailureTypeException {

    public InvalidObjectException(String entityType, String detail) {
        super(
                getCode(entityType),
                detail,
                FailureType.NOT_RETRYABLE
        );
    }

    private static String getCode(String entityType){
        return entityType + ".invalidObject";
    }
}
