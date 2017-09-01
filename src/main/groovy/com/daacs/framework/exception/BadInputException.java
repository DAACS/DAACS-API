package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 7/25/16.
 */
public class BadInputException extends FailureTypeException {

    public BadInputException(String entityType, String detail) {
        super(getCode(entityType), detail, FailureType.NOT_RETRYABLE, getMeta(entityType));
    }

    private static String getCode(String entityType){
        return entityType + ".alreadyExists";
    }

    private static Map<String, Object> getMeta(String entityType){
        Map<String, Object> meta = new HashMap<>();
        meta.put("entity_type", entityType);

        return meta;
    }
}
