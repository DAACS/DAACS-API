package com.daacs.framework.hystrix;

import com.daacs.framework.exception.ErrorContainerException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 6/23/16.
 */
public class FailureTypeException extends ErrorContainerException {

    protected FailureType failureType;
    protected boolean log = true;

    public FailureTypeException(String code, String detail, FailureType failureType, Map<String, Object> metaData, Throwable cause) {
        super(code, detail, getMeta(failureType, metaData), cause);
        this.failureType = failureType;
    }

    public FailureTypeException(String code, String detail, FailureType failureType, Map<String, Object> metaData) {
        this(code, detail, failureType, metaData, null);
    }

    public FailureTypeException(String code, String detail, FailureType failureType, Throwable cause) {
        this(code, detail, failureType, new HashMap<>(), cause);
    }

    public FailureTypeException(String code, String detail, FailureType failureType) {
        this(code, detail, failureType, new HashMap<>(), null);
    }

    public FailureType getFailureType() {
        return failureType;
    }

    private static Map<String, Object> getMeta(FailureType failureType, Map<String, Object> metaData){
        metaData.put("failure_type", failureType.toString());
        return metaData;
    }

    public boolean getLog() {
        return log;
    }

    public void setLog(boolean log) {
        this.log = log;
    }
}
