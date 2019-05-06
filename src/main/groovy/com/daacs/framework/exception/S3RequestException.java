package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 12/22/16.
 */
public class S3RequestException extends FailureTypeException {

    public S3RequestException(String hystrixCommandKey, FailureType failureType, Throwable cause) {
        super(
                getCode(),
                getDetail(hystrixCommandKey, cause),
                failureType,
                getMeta(hystrixCommandKey),
                cause
        );
    }

    private static String getCode() {
        return "S3.requestFailed";
    }

    private static String getDetail(String hystrixCommandKey, Throwable cause) {
        return "Failed making a request to S3 using hystrix command " + hystrixCommandKey +(cause == null ? "" : ": " + cause.getMessage());
    }

    private static Map<String, Object> getMeta(String hystrixCommandKey) {
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("hystrix_command_key", hystrixCommandKey);
        return metaData;
    }
}