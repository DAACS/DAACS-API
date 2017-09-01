package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 12/5/16.
 */
public class UnexpectedHttpStatusException extends FailureTypeException {

    public UnexpectedHttpStatusException(String entityType, HttpStatus httpStatus, String responseBody, FailureType failureType) {
        super(
                getCode(entityType),
                getDetail(httpStatus),
                failureType,
                getMeta(entityType, httpStatus, responseBody)
        );
    }

    private static String getCode(String entityType){
        return entityType + ".unexpectedHttpStatus";
    }
    private static String getDetail(HttpStatus httpStatus){
        return "Unexpected HTTP response from server: " + httpStatus.toString();
    }
    private static Map<String, Object> getMeta(String entityType, HttpStatus httpStatus, String responseBody){
        Map<String, Object> meta = new HashMap<>();
        meta.put("entity_type", entityType);
        meta.put("httpStatus", httpStatus);
        meta.put("responseBody", responseBody);

        return meta;
    }
}
