package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

/**
 * Created by chostetter on 7/25/16.
 */
public class RepoNotFoundException extends FailureTypeException {

    public RepoNotFoundException(String entityType) {
        super(
                getCode(entityType),
                getDetail(entityType),
                FailureType.NOT_RETRYABLE
        );
    }

    public static String getCode(String entityType){
        return entityType + ".notFound";
    }

    public static String getDetail(String entityType){
        return "Could not find " + entityType;
    }
}
