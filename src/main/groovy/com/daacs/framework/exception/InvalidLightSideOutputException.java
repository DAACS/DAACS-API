package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 7/25/16.
 */
public class InvalidLightSideOutputException extends FailureTypeException {

    public InvalidLightSideOutputException(String file) {
        super(
                getCode(),
                getDetail(file),
                FailureType.NOT_RETRYABLE,
                getMeta(file)
        );
    }

    public InvalidLightSideOutputException(String file, Throwable t) {
        super(
                getCode(),
                getDetail(file),
                FailureType.NOT_RETRYABLE,
                getMeta(file),
                t
        );
    }

    public static String getCode(){
        return "lightside.invalidOutput";
    }

    private static String getDetail(String file){
        return "Invalid LightSide output: " + file;
    }

    private static Map<String, Object> getMeta(String file){
        Map<String, Object> meta = new HashMap<>();
        meta.put("file", file);

        return meta;
    }
}
