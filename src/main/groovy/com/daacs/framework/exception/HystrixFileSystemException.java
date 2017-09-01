package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 7/25/16.
 */
public class HystrixFileSystemException extends FailureTypeException {

    public HystrixFileSystemException(String resourceName, String hystrixCommandKey, FailureType failureType, Map<String, Object> metaData, Throwable t) {
        super(
                getCode(),
                getDetail(resourceName, hystrixCommandKey, t),
                failureType,
                getMeta(resourceName, hystrixCommandKey, metaData),
                t
        );
    }

    public static String getCode(){
        return "fileSystem.exception";
    }

    private static String getDetail(String resourceName, String hystrixCommandKey, Throwable cause){
        return  "Failed to execute command for " + resourceName + " using hystrix command " + hystrixCommandKey + (cause == null ? "" : ": " + cause.getMessage());
    }

    private static Map<String, Object> getMeta(String resourceName, String hystrixCommandKey, Map<String, Object> otherMetaData){
        Map<String, Object> metaData = new HashMap<>();
        metaData.put("resource_name", resourceName);
        metaData.put("hystrix_command_key", hystrixCommandKey);
        metaData.putAll(otherMetaData);
        return metaData;
    }
}
