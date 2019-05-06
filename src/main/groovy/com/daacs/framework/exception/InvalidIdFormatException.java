package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mgoldman on 3/13/19.
 */
public class InvalidIdFormatException extends FailureTypeException {


    public InvalidIdFormatException(String id) {
        super(
                getCode(),
                getDetail(id),
                FailureType.NOT_RETRYABLE,
                getMeta(id)
        );

    }

    public InvalidIdFormatException(String id, Throwable throwable) {
        super(
                getCode(),
                getDetail(id),
                FailureType.NOT_RETRYABLE,
                getMeta(id),
                throwable
        );

    }

    public static String getCode(){
        return "id.invalid";
    }

    private static String getDetail(String id){
        return "Invalid id: " + id;
    }

    private static Map<String, Object> getMeta(String id){
        Map<String, Object> meta = new HashMap<>();
        meta.put("id", id);

        return meta;
    }
}
