package com.daacs.framework.exception;

import com.daacs.model.ErrorContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by chostetter on 6/23/16.
 */
public class ErrorContainerException extends RuntimeException {

    protected List<ErrorContainer> errorContainers;

    public ErrorContainerException(String code, String detail, Map<String, Object> metaData) {
        this(code, detail, metaData, null);
    }

    public ErrorContainerException(String code, String detail, Map<String, Object> metaData, Throwable cause) {
        super(code + ": " + detail, cause);
        this.errorContainers = new ArrayList<>();

        ErrorContainer errorContainer = new ErrorContainer();
        errorContainer.setCode(code);
        errorContainer.setDetail(detail);
        errorContainer.setMeta(metaData);
        this.errorContainers.add(errorContainer);
    }

    public ErrorContainerException(String code, String detail, List<ErrorContainer> errorContainers) {
        super(code + ": " + detail, null);
        this.errorContainers = errorContainers;
    }

    public List<ErrorContainer> getErrorContainers() {
        return errorContainers;
    }
}
