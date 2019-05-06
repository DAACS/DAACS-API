package com.daacs.framework.exception;

import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 1/3/17.
 */
public class GenericFileException extends FailureTypeException {

    public GenericFileException(String fileName, Throwable cause) {
        super(
                getCode(),
                getDetail(),
                FailureType.NOT_RETRYABLE,
                getMeta(fileName),
                cause
        );
    }

    private static String getCode() {
        return "file.error";
    }

    private static String getDetail() {
        return "Encountered a file error";
    }

    private static Map<String, Object> getMeta(String fileName) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("file_name", fileName);

        return meta;
    }
}