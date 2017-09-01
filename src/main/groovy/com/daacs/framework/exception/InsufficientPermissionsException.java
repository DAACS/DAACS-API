package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

/**
 * Created by chostetter on 7/25/16.
 */
public class InsufficientPermissionsException extends FailureTypeException {

    public InsufficientPermissionsException(String entityType) {
        super(
                getCode(entityType),
                getDetail(entityType),
                FailureType.NOT_RETRYABLE
        );
    }

    private static String getCode(String entityType){
        return entityType + ".insufficientPermissions";
    }

    private static String getDetail(String entityType){
        return entityType + " does not have sufficient permissions";
    }
}
