package com.daacs.framework.hystrix;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 6/23/16.
 */
public class HystrixRequestException extends FailureTypeException {

    public HystrixRequestException(String resourceName, String hystrixCommandKey, FailureType failureType, Throwable cause) {
        super(
                getCode(resourceName),
                getDetail(resourceName, hystrixCommandKey, cause),
                failureType,
                getMeta(resourceName, hystrixCommandKey),
                cause
        );
    }

    private static String getCode(String resourceName){
        return resourceName + ".requestFailed";
    }

    private static String getDetail(String resourceName, String hystrixCommandKey, Throwable cause){
        return  "Failed making a request to " + resourceName + " using hystrix command " + hystrixCommandKey + (cause == null ? "" : ": " + cause.getMessage());
    }

    private static Map<String, Object> getMeta(String resourceName, String hystrixCommandKey){
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("resource_name", resourceName);
        metaData.put("hystrix_command_key", hystrixCommandKey);
        return metaData;
    }
}
