package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by alandistasio on 10/23/15.
 */
public class AlreadyExistsException extends FailureTypeException {

    public AlreadyExistsException(String entityType, String entityIdentifier, String identifierValue) {
        super(
                getCode(entityType),
                getDetail(entityType, entityIdentifier, identifierValue),
                FailureType.NOT_RETRYABLE,
                getMeta(entityType, entityIdentifier, identifierValue)
        );
    }

    private static String getCode(String entityType){
        return entityType + ".alreadyExists";
    }

    private static String getDetail(String entityType, String entityIdentifier, String identifierValue){
        return entityType + " already exists for " + entityIdentifier + "=" + identifierValue;
    }

    private static Map<String, Object> getMeta(String entityType, String entityIdentifier, String identifierValue){
        Map<String, Object> meta = new HashMap<>();
        meta.put("entity_type", entityType);
        meta.put(entityIdentifier, identifierValue);

        return meta;
    }
}
