package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 7/25/16.
 */
public class InvalidDirectoryException extends FailureTypeException {


    public InvalidDirectoryException(String directory) {
        super(
                getCode(),
                getDetail(directory),
                FailureType.NOT_RETRYABLE,
                getMeta(directory)
        );

    }

    public InvalidDirectoryException(String directory, Throwable throwable) {
        super(
                getCode(),
                getDetail(directory),
                FailureType.NOT_RETRYABLE,
                getMeta(directory),
                throwable
        );

    }

    public static String getCode(){
        return "directory.invalid";
    }

    private static String getDetail(String directory){
        return "Invalid directory: " + directory;
    }

    private static Map<String, Object> getMeta(String directory){
        Map<String, Object> meta = new HashMap<>();
        meta.put("directory", directory);

        return meta;
    }
}
